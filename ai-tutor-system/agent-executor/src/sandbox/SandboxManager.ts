import { $ } from 'bun';
import { logger } from '../utils/logger';

/**
 * 沙箱配置
 */
export interface SandboxConfig {
  path: string;
  workingDirectory: string;
  memoryLimit: string;
  cpuLimit: number;
  timeoutMs: number;
}

/**
 * 沙箱实例
 */
export interface Sandbox {
  id: string;
  path: string;
  workingDirectory: string;
  createdAt: Date;
}

/**
 * 沙箱管理器
 */
export class SandboxManager {
  private sandboxes: Map<string, Sandbox> = new Map();
  private readonly baseSandboxPath = '/tmp/agent-sandbox';
  
  /**
   * 创建沙箱
   */
  async createSandbox(sessionId: string): Promise<Sandbox> {
    const sandboxPath = `${this.baseSandboxPath}/${sessionId}`;
    
    try {
      await $`mkdir -p ${sandboxPath}`;
      
      const sandbox: Sandbox = {
        id: sessionId,
        path: sandboxPath,
        workingDirectory: sandboxPath,
        createdAt: new Date()
      };
      
      this.sandboxes.set(sessionId, sandbox);
      
      logger.info(`Sandbox created: ${sandboxPath}`);
      
      return sandbox;
    } catch (error) {
      logger.error(`Failed to create sandbox: ${error}`);
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
      await $`rm -rf ${sandbox.path}`;
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
    for (const [sessionId] of this.sandboxes) {
      await this.deleteSandbox(sessionId);
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
}
