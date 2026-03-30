import { $ } from 'bun';
import { logger } from '../utils/logger';

/**
 * cgroups v2 资源限制
 */
export interface CgroupLimits {
  /** 最大内存（字节），如 536870912 = 512MB */
  memoryMax?: number;
  /** 最大内存+交换空间（字节） */
  memorySwapMax?: number;
  /** CPU 权重 (1-10000) */
  cpuWeight?: number;
  /** CPU 最大额度 */
  cpuMax?: string;  // e.g., "100000 100000" = 100ms per 100ms
  /** 最大进程数 */
  pidsMax?: number;
  /** IO 权重 (1-10000) */
  ioWeight?: number;
  /** IO 带宽限制，如 "1048576" 1MB/s */
  ioMaxBps?: string;
  /** IO 带宽读取限制 */
  ioMaxRbps?: string;
  /** IO 带宽写入限制 */
  ioMaxWbps?: string;
}

/**
 * cgroups v2 层级路径
 */
export interface CgroupHierarchy {
  root: string;
  memoryPath: string;
  cpuPath: string;
  pidsPath: string;
  ioPath: string;
}

/**
 * cgroups v2 资源管理器
 * 
 * 支持 cgroups v2（统一层级），路径格式：
 * /sys/fs/cgroup/<controller>/<group-name>/
 */
export class CgroupManager {
  private readonly basePath: string;
  private readonly groupName: string;
  private hierarchy: CgroupHierarchy;
  private enabled: boolean = false;

  constructor(basePath: string = '/sys/fs/cgroup', groupName: string = 'agent-sandbox') {
    this.basePath = basePath;
    this.groupName = `${groupName}-${Date.now()}-${Math.random().toString(36).substring(7)}`;
    this.hierarchy = {
      root: `${basePath}`,
      memoryPath: '',
      cpuPath: '',
      pidsPath: '',
      ioPath: '',
    };
  }

  /**
   * 检查 cgroups v2 是否可用
   */
  static async isAvailable(): Promise<{ available: boolean; version: 'v1' | 'v2' | 'none' }> {
    try {
      // 检查 cgroups v2
      const v2Result = await $`test -d /sys/fs/cgroup/unified && echo "v2"`.quiet();
      if (v2Result.exitCode === 0) {
        return { available: true, version: 'v2' };
      }

      // 检查 cgroups v1
      const v1Result = await $`test -d /sys/fs/cgroup/memory && echo "v1"`.quiet();
      if (v1Result.exitCode === 0) {
        return { available: true, version: 'v1' };
      }

      return { available: false, version: 'none' };
    } catch {
      return { available: false, version: 'none' };
    }
  }

  /**
   * 检查是否以 root 权限运行
   */
  static isRoot(): boolean {
    return process.getuid?.() === 0 || process.env.USER === 'root';
  }

  /**
   * 初始化 cgroup（创建层级目录）
   */
  async init(limits: CgroupLimits = {}): Promise<boolean> {
    if (!CgroupManager.isRoot()) {
      logger.warn('cgroups: Not running as root, resource limits will not be applied');
      return false;
    }

    const cgroupsInfo = await CgroupManager.isAvailable();
    if (!cgroupsInfo.available) {
      logger.warn('cgroups: Not available on this system');
      return false;
    }

    try {
      const groupPath = `${this.basePath}/${this.groupName}`;

      // 创建 cgroup 目录
      await $`mkdir -p ${groupPath}`.quiet();

      // 设置资源限制
      await this.setLimits(limits);

      this.enabled = true;
      logger.info(`cgroups: Initialized group ${this.groupName}`);

      return true;
    } catch (error) {
      logger.error('cgroups: Failed to initialize:', error);
      return false;
    }
  }

  /**
   * 设置资源限制
   */
  async setLimits(limits: CgroupLimits): Promise<void> {
    const groupPath = `${this.basePath}/${this.groupName}`;

    // 内存限制
    if (limits.memoryMax) {
      await this.writeFile(`${groupPath}/memory.max`, String(limits.memoryMax));
    }

    if (limits.memorySwapMax) {
      await this.writeFile(`${groupPath}/memory.swap.max`, String(limits.memorySwapMax));
    }

    // CPU 限制
    if (limits.cpuWeight) {
      await this.writeFile(`${groupPath}/cpu.weight`, String(limits.cpuWeight));
    }

    if (limits.cpuMax) {
      await this.writeFile(`${groupPath}/cpu.max`, limits.cpuMax);
    }

    // 进程数限制
    if (limits.pidsMax) {
      await this.writeFile(`${groupPath}/pids.max`, String(limits.pidsMax));
    }

    // IO 限制
    if (limits.ioWeight) {
      await this.writeFile(`${groupPath}/io.weight`, String(limits.ioWeight));
    }

    if (limits.ioMaxBps) {
      // 格式: "deviceIO max"
      await this.writeFile(`${groupPath}/io.max`, limits.ioMaxBps);
    }
  }

  /**
   * 获取当前 cgroup 路径
   */
  getGroupPath(): string {
    return `${this.basePath}/${this.groupName}`;
  }

  /**
   * 获取当前组的 PID
   */
  async getCurrentPids(): Promise<number[]> {
    try {
      const pidsPath = `${this.getGroupPath()}/cgroup.procs`;
      const content = await Bun.file(pidsPath).text();
      return content.trim().split('\n').filter(Boolean).map(Number);
    } catch {
      return [];
    }
  }

  /**
   * 将当前进程添加到 cgroup
   */
  async addCurrentProcess(): Promise<void> {
    if (!this.enabled) return;

    try {
      const pid = process.pid;
      const cgroupProcs = `${this.getGroupPath()}/cgroup.procs`;
      await this.writeFile(cgroupProcs, String(pid));
      logger.info(`cgroups: Added process ${pid} to ${this.groupName}`);
    } catch (error) {
      logger.error('cgroups: Failed to add current process:', error);
    }
  }

  /**
   * 将指定 PID 添加到 cgroup（用于子进程）
   */
  async addProcess(pid: number): Promise<void> {
    if (!this.enabled) return;

    try {
      const cgroupProcs = `${this.getGroupPath()}/cgroup.procs`;
      await this.writeFile(cgroupProcs, String(pid));
    } catch (error) {
      logger.error(`cgroups: Failed to add process ${pid}:`, error);
    }
  }

  /**
   * 获取资源使用统计
   */
  async getUsage(): Promise<{
    memory?: { current: number; peak: number };
    cpu?: { usage: string };
    pids?: { current: number; max: number };
  }> {
    const groupPath = this.getGroupPath();
    const usage: any = {};

    try {
      // 内存使用
      const memoryCurrent = await this.readFile(`${groupPath}/memory.current`).catch(() => null);
      const memoryPeak = await this.readFile(`${groupPath}/memory.peak`).catch(() => null);
      if (memoryCurrent) {
        usage.memory = {
          current: parseInt(memoryCurrent) || 0,
          peak: parseInt(memoryPeak) || 0,
        };
      }

      // CPU 使用
      const cpuUsage = await this.readFile(`${groupPath}/cpu.stat`).catch(() => null);
      if (cpuUsage) {
        const usageNsec = cpuUsage.match(/usage_usec (\d+)/)?.[1];
        if (usageNsec) {
          usage.cpu = { usage: usageNsec };
        }
      }

      // 进程数
      const pidsCurrent = await this.readFile(`${groupPath}/pids.current`).catch(() => null);
      const pidsMax = await this.readFile(`${groupPath}/pids.max`).catch(() => null);
      if (pidsCurrent) {
        usage.pids = {
          current: parseInt(pidsCurrent) || 0,
          max: parseInt(pidsMax) || 0,
        };
      }
    } catch (error) {
      logger.error('cgroups: Failed to get usage:', error);
    }

    return usage;
  }

  /**
   * 清理 cgroup（终止所有进程并删除）
   */
  async cleanup(): Promise<void> {
    if (!this.enabled) return;

    try {
      const groupPath = this.getGroupPath();

      // 终止所有进程
      const pids = await this.getCurrentPids();
      for (const pid of pids) {
        try {
          // 尝试 SIGTERM
          process.kill(pid, 'SIGTERM');
        } catch {
          // 进程可能已经结束
        }
      }

      // 等待进程退出
      if (pids.length > 0) {
        await new Promise(resolve => setTimeout(resolve, 500));
        // 强制终止剩余进程
        for (const pid of await this.getCurrentPids()) {
          try {
            process.kill(pid, 'SIGKILL');
          } catch {
            // 可能已结束
          }
        }
      }

      // 删除 cgroup
      await $`rmdir ${groupPath}`.quiet();
      logger.info(`cgroups: Cleaned up group ${this.groupName}`);
    } catch (error) {
      logger.error('cgroups: Failed to cleanup:', error);
    }
  }

  /**
   * 写入 cgroup 文件
   */
  private async writeFile(path: string, value: string): Promise<void> {
    try {
      await Bun.write(path, value);
    } catch (error) {
      logger.error(`cgroups: Failed to write ${path}:`, error);
      throw error;
    }
  }

  /**
   * 读取 cgroup 文件
   */
  private async readFile(path: string): Promise<string> {
    try {
      return await Bun.file(path).text();
    } catch (error) {
      logger.error(`cgroups: Failed to read ${path}:`, error);
      throw error;
    }
  }
}

/**
 * 预设的 cgroups 限制配置
 */
export const CGROUP_PRESETS = {
  // 严格限制（适合不受信任的代码）
  strict: {
    memoryMax: 256 * 1024 * 1024,      // 256MB
    memorySwapMax: 256 * 1024 * 1024,  // 不允许交换
    cpuWeight: 100,
    cpuMax: '50000 100000',             // 50% CPU
    pidsMax: 64,
    ioWeight: 100,
  } as CgroupLimits,

  // 标准限制（适合一般任务）
  standard: {
    memoryMax: 512 * 1024 * 1024,      // 512MB
    memorySwapMax: 1024 * 1024 * 1024, // 1GB 交换
    cpuWeight: 512,
    cpuMax: '100000 100000',           // 100% CPU
    pidsMax: 256,
    ioWeight: 1000,
  } as CgroupLimits,

  // 宽松限制（适合编译等重型任务）
  relaxed: {
    memoryMax: 2 * 1024 * 1024 * 1024, // 2GB
    memorySwapMax: 4 * 1024 * 1024 * 1024, // 4GB 交换
    cpuWeight: 2048,
    cpuMax: '200000 100000',           // 200% CPU
    pidsMax: 1024,
    ioWeight: 5000,
  } as CgroupLimits,
};
