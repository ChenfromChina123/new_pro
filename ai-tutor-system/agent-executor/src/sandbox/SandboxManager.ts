import { $ } from 'bun';
import { logger } from '../utils/logger';
import { BubblewrapExecutor, BubblewrapLevel } from './Bubblewrap';
import { CgroupManager, CgroupLimits, CGROUP_PRESETS } from './CgroupManager';

/**
 * 沙箱隔离级别
 */
export type SandboxLevel = 'basic' | 'standard' | 'strict';

/**
 * 沙箱配置
 */
export interface SandboxConfig {
  path: string;
  workingDirectory: string;
  memoryLimit: string;
  cpuLimit: number;
  timeoutMs: number;
  isolationLevel: SandboxLevel;
  bwrapEnabled: boolean;
  cgroupEnabled: boolean;
  networkEnabled: boolean;
}

/**
 * 沙箱实例
 */
export interface Sandbox {
  id: string;
  path: string;
  workingDirectory: string;
  createdAt: Date;
  isolationLevel: SandboxLevel;
  bwrapEnabled: boolean;
  cgroupEnabled: boolean;
  cgroupPath?: string;
}

/**
 * 沙箱管理器
 * 
 * 使用 bwrap + cgroups 实现容器级隔离
 */
export class SandboxManager {
  private sandboxes: Map<string, Sandbox> = new Map();
  private readonly baseSandboxPath = '/tmp/agent-sandbox';

  /**
   * 检查系统隔离能力
   */
  async checkCapabilities(): Promise<{
    bwrapAvailable: boolean;
    cgroupAvailable: boolean;
    cgroupVersion: 'v1' | 'v2' | 'none';
    isRoot: boolean;
  }> {
    const [bwrapAvailable, cgroupInfo] = await Promise.all([
      BubblewrapExecutor.isAvailable(),
      CgroupManager.isAvailable(),
    ]);

    return {
      bwrapAvailable,
      cgroupAvailable: cgroupInfo.available,
      cgroupVersion: cgroupInfo.version,
      isRoot: CgroupManager.isRoot(),
    };
  }

  /**
   * 创建沙箱
   */
  async createSandbox(
    sessionId: string,
    options: {
      isolationLevel?: SandboxLevel;
      limits?: CgroupLimits;
      networkEnabled?: boolean;
    } = {}
  ): Promise<Sandbox> {
    const {
      isolationLevel = 'standard',
      limits = CGROUP_PRESETS.standard,
      networkEnabled = isolationLevel !== 'strict',
    } = options;

    const sandboxPath = `${this.baseSandboxPath}/${sessionId}`;
    const capabilities = await this.checkCapabilities();

    let cgroupPath: string | undefined;
    let cgroupEnabled = false;

    try {
      // 1. 创建沙箱目录
      await $`mkdir -p ${sandboxPath}`.quiet();

      // 2. 初始化 cgroups（如果可用且有 root 权限）
      if (capabilities.cgroupAvailable && capabilities.isRoot) {
        try {
          const cgroupManager = new CgroupManager('/sys/fs/cgroup', `agent-${sessionId}`);
          const initSuccess = await cgroupManager.init(limits);
          if (initSuccess) {
            cgroupPath = cgroupManager.getGroupPath();
            cgroupEnabled = true;
            
            // 将当前进程加入 cgroup
            await cgroupManager.addCurrentProcess();
            
            // 存储 cgroup manager 引用（未来用于进程管理）
            (globalThis as any)[`cgroup_${sessionId}`] = cgroupManager;
          }
        } catch (error) {
          logger.warn(`cgroups: Failed to initialize, continuing without cgroups: ${error}`);
        }
      }

      // 3. 初始化 Git 仓库
      await this.initGitRepo(sandboxPath);

      // 4. 创建沙箱元数据文件
      const meta = {
        id: sessionId,
        path: sandboxPath,
        isolationLevel,
        bwrapEnabled: capabilities.bwrapAvailable,
        cgroupEnabled,
        createdAt: new Date().toISOString(),
        networkEnabled,
      };
      await Bun.write(`${sandboxPath}/.sandbox-meta.json`, JSON.stringify(meta, null, 2));

      const sandbox: Sandbox = {
        id: sessionId,
        path: sandboxPath,
        workingDirectory: sandboxPath,
        createdAt: new Date(),
        isolationLevel,
        bwrapEnabled: capabilities.bwrapAvailable,
        cgroupEnabled,
        cgroupPath,
      };

      this.sandboxes.set(sessionId, sandbox);
      logger.info(`Sandbox created: ${sandboxPath} (level=${isolationLevel}, bwrap=${capabilities.bwrapAvailable}, cgroup=${cgroupEnabled})`);

      return sandbox;
    } catch (error) {
      logger.error(`Failed to create sandbox: ${error}`);
      // 清理已创建的资源
      await this.cleanupPartial(sessionId, cgroupPath);
      throw error;
    }
  }

  /**
   * 获取沙箱
   */
  getSandbox(sessionId: string): Sandbox | undefined {
    return this.sandboxes.get(sessionId);
  }

  /**
   * 删除沙箱
   */
  async deleteSandbox(sessionId: string): Promise<void> {
    const sandbox = this.sandboxes.get(sessionId);
    if (!sandbox) {
      return;
    }

    try {
      // 1. 清理 cgroup（如果有）
      const cgroupKey = `cgroup_${sessionId}`;
      const cgroupManager = (globalThis as any)[cgroupKey];
      if (cgroupManager) {
        await cgroupManager.cleanup();
        delete (globalThis as any)[cgroupKey];
      }

      // 2. 删除沙箱目录
      await $`rm -rf ${sandbox.path}`.quiet();
      this.sandboxes.delete(sessionId);

      logger.info(`Sandbox deleted: ${sandbox.path}`);
    } catch (error) {
      logger.error(`Failed to delete sandbox: ${error}`);
      throw error;
    }
  }

  /**
   * 清理所有沙箱
   */
  async cleanup(): Promise<void> {
    const sessionIds = Array.from(this.sandboxes.keys());
    for (const sessionId of sessionIds) {
      await this.deleteSandbox(sessionId);
    }
  }

  /**
   * 在沙箱中执行命令（带隔离）
   */
  async executeInSandbox(
    sessionId: string,
    command: string,
    options: {
      timeoutMs?: number;
      networkEnabled?: boolean;
    } = {}
  ): Promise<{ stdout: string; stderr: string; exitCode: number }> {
    const sandbox = this.getSandbox(sessionId);
    if (!sandbox) {
      throw new Error(`Sandbox not found for session: ${sessionId}`);
    }

    const capabilities = await this.checkCapabilities();

    // 如果 bwrap 可用，使用隔离执行
    if (capabilities.bwrapAvailable && sandbox.bwrapEnabled) {
      return BubblewrapExecutor.exec(
        command,
        sandbox.path,
        sandbox.isolationLevel as BubblewrapLevel,
        {
          networkEnabled: options.networkEnabled ?? sandbox.isolationLevel !== 'strict',
          timeoutMs: options.timeoutMs,
        }
      );
    }

    // 降级：直接执行（仅文件系统隔离）
    const timeout = options.timeoutMs || 60000;
    try {
      const result = await $`${command}`.cwd(sandbox.path).timeout(timeout);
      return {
        stdout: result.stdout.toString(),
        stderr: result.stderr.toString(),
        exitCode: result.exitCode,
      };
    } catch (error: any) {
      if (error.exitCode !== undefined) {
        return {
          stdout: error.stdout?.toString() || '',
          stderr: error.stderr?.toString() || '',
          exitCode: error.exitCode,
        };
      }
      throw error;
    }
  }

  /**
   * 初始化 Git 仓库
   */
  async initGitRepo(sandboxPath: string): Promise<void> {
    try {
      await $`git init`.cwd(sandboxPath);
      await $`git config user.email "agent@local"`.cwd(sandboxPath);
      await $`git config user.name "Agent"`.cwd(sandboxPath);

      const gitignore = `node_modules/
dist/
*.log
.DS_Store
.cache/
`;
      await Bun.write(`${sandboxPath}/.gitignore`, gitignore);

      await $`git add .`.cwd(sandboxPath);
      await $`git commit -m "Initial snapshot"`.cwd(sandboxPath);

      logger.info(`Git repository initialized in ${sandboxPath}`);
    } catch (error) {
      logger.error(`Failed to init git repo: ${error}`);
      throw error;
    }
  }

  /**
   * 创建 Git 快照
   */
  async createGitSnapshot(sandboxPath: string, message: string): Promise<string> {
    try {
      await $`git add .`.cwd(sandboxPath);
      await $`git commit -m ${message} --allow-empty`.cwd(sandboxPath);

      const result = await $`git rev-parse HEAD`.cwd(sandboxPath);
      const commitHash = result.text().trim();

      logger.info(`Git snapshot created: ${commitHash}`);
      return commitHash;
    } catch (error) {
      logger.error(`Failed to create git snapshot: ${error}`);
      throw error;
    }
  }

  /**
   * 回滚到上一个快照
   */
  async rollbackGit(sandboxPath: string): Promise<void> {
    try {
      await $`git reset --hard HEAD^`.cwd(sandboxPath);
      logger.info(`Git rollback completed in ${sandboxPath}`);
    } catch (error) {
      logger.error(`Failed to rollback git: ${error}`);
      throw error;
    }
  }

  /**
   * 获取沙箱资源使用情况
   */
  async getSandboxUsage(sessionId: string): Promise<{
    sandbox?: Sandbox;
    usage?: {
      memory?: { current: number; peak: number };
      cpu?: { usage: string };
      pids?: { current: number; max: number };
    };
  }> {
    const sandbox = this.getSandbox(sessionId);
    if (!sandbox) {
      return { sandbox: undefined, usage: undefined };
    }

    const cgroupKey = `cgroup_${sessionId}`;
    const cgroupManager = (globalThis as any)[cgroupKey];

    if (cgroupManager) {
      const usage = await cgroupManager.getUsage();
      return { sandbox, usage };
    }

    return { sandbox, usage: undefined };
  }

  /**
   * 清理部分创建的资源（用于错误恢复）
   */
  private async cleanupPartial(sessionId: string, cgroupPath?: string): Promise<void> {
    try {
      if (cgroupPath) {
        await $`rmdir ${cgroupPath}`.quiet().catch(() => {});
      }
      await $`rm -rf ${this.baseSandboxPath}/${sessionId}`.quiet().catch(() => {});
    } catch {
      // 忽略清理错误
    }
  }
}