import { $ } from 'bun';
import { logger } from '../utils/logger';

/**
 * bwrap 隔离级别
 */
export type BubblewrapLevel = 'basic' | 'standard' | 'strict';

/**
 * bwrap 隔离配置
 */
export interface BubblewrapConfig {
  level: BubblewrapLevel;
  sandboxPath: string;
  readonly: boolean;
  allowedDirs: string[];
  networkEnabled: boolean;
  tmpfsSize?: string;
}

/**
 * bwrap 命令生成器
 */
export class BubblewrapBuilder {
  private args: string[] = [];
  private sandboxPath: string;
  private readonly: boolean;

  constructor(sandboxPath: string, readonly: boolean = true) {
    this.sandboxPath = sandboxPath;
    this.readonly = readonly;
  }

  /**
   * 创建新用户命名空间（隔离 PID、网络）
   */
  unshareUser(): this {
    this.args.push('--unshare-user');
    return this;
  }

  /**
   * 创建新 PID 命名空间（隔离进程）
   */
  unsharePid(): this {
    this.args.push('--unshare-pid');
    return this;
  }

  /**
   * 创建新网络命名空间
   */
  unshareNet(): this {
    this.args.push('--unshare-net');
    return this;
  }

  /**
   * 创建新的 IPC 命名空间
   */
  unshareIpc(): this {
    this.args.push('--unshare-ipc');
    return this;
  }

  /**
   * 设置工作目录
   */
  chdir(path: string): this {
    this.args.push(`--chdir`, path);
    return this;
  }

  /**
   * 设置根目录为只读
   */
  ro-bind(path: string, target: string): this {
    this.args.push(`--ro-bind`, path, target);
    return this;
  }

  /**
   * 绑定挂载可写目录
   */
  bind(path: string, target: string): this {
    this.args.push(`--bind`, path, target);
    return this;
  }

  /**
   * 创建 tmpfs 挂载
   */
  tmpfs(path: string, size?: string): this {
    this.args.push(`--tmpfs`);
    if (size) {
      this.args.push(`${path}`, '-o', `size=${size}`);
    } else {
      this.args.push(path);
    }
    return this;
  }

  /**
   * 创建 dev 目录（最小设备节点）
   */
  dev(): this {
    this.args.push('--dev');
    return this;
  }

  /**
   * 创建 /dev/null
   */
  devNull(): this {
    this.args.push('--dev-bind', '/dev/null', '/dev/null');
    return this;
  }

  /**
   * 设置 hostname
   */
  setHostname(name: string): this {
    this.args.push(`--hostname`, name);
    return this;
  }

  /**
   * 添加环境变量
   */
  setEnv(key: string, value: string): this {
    this.args.push(`--setenv`, key, value);
    return this;
  }

  /**
   * 设置用户 ID
   */
  setUid(uid: number): this {
    this.args.push(`--uid`, String(uid));
    return this;
  }

  /**
   * 设置组 ID
   */
  setGid(gid: number): this {
    this.args.push(`--gid`, String(gid));
    return this;
  }

  /**
   * 禁用新挂载
   */
  disallowNewPrivs(): this {
    this.args.push('--disallow-new-priviledges');
    return this;
  }

  /**
   * 设置 seccomp 白名单
   */
  seccomp(seccompFile?: string): this {
    this.args.push('--seccomp');
    if (seccompFile) {
      this.args.push(seccompFile);
    }
    return this;
  }

  /**
   * 添加不可绑定目录
   */
  unshareCgroup(): this {
    this.args.push('--unshare-cgroup');
    return this;
  }

  /**
   * 设置挂载点
   */
  mount(dest: string, fsType: string, source?: string, options?: string): this {
    this.args.push('--mount');
    if (source) {
      const mountStr = `${source}=${dest}`;
      if (options) {
        this.args.push(mountStr, '-t', fsType, '-o', options);
      } else {
        this.args.push(mountStr, '-t', fsType);
      }
    } else {
      this.args.push(dest, '-t', fsType);
    }
    return this;
  }

  /**
   * 设置进程上限
   */
  limitAs(limit: string): this {
    this.args.push('--limit-as', limit);
    return this;
  }

  /**
   * 设置文件数上限
   */
  limitNofile(limit: number): this {
    this.args.push('--limit-nofile', String(limit));
    return this;
  }

  /**
   * 构建命令
   */
  build(command: string[]): string[] {
    return ['bwrap', ...this.args, '--', ...command];
  }
}

/**
 * Bubblewrap 隔离执行器
 */
export class BubblewrapExecutor {
  private static readonly SYSTEM_DIRS = [
    '/bin',
    '/usr/bin',
    '/usr/sbin',
    '/lib',
    '/lib64',
    '/usr/lib',
    '/usr/local/bin',
    '/etc/alternatives',
  ];

  /**
   * 检查 bwrap 是否可用
   */
  static async isAvailable(): Promise<boolean> {
    try {
      const result = await $`which bwrap`.quiet();
      return result.exitCode === 0;
    } catch {
      return false;
    }
  }

  /**
   * 获取系统必要目录列表
   */
  static getSystemDirs(): string[] {
    return [...this.SYSTEM_DIRS];
  }

  /**
   * 创建严格隔离的沙箱路径
   */
  static buildStrictSandbox(config: BubblewrapConfig): string[] {
    const { sandboxPath, readonly, allowedDirs } = config;
    const builder = new BubblewrapBuilder(sandboxPath, readonly);

    // 基础隔离
    builder.unshareUser()
           .unsharePid()
           .unshareIpc()
           .unshareNet()  // 无网络
           .unshareCgroup()
           .disallowNewPrivs();

    // 设置 hostname
    builder.setHostname(`agent-sandbox-${Date.now()}`);

    // 环境变量
    builder.setEnv('HOME', sandboxPath)
           .setEnv('USER', 'agent')
           .setEnv('PATH', '/usr/local/bin:/usr/bin:/bin')
           .setEnv('LANG', 'en_US.UTF-8')
           .setEnv('TERM', 'xterm-256color');

    // 工作目录
    builder.chdir(sandboxPath);

    // 挂载系统只读目录
    const dirsToMount = allowedDirs.length > 0 ? allowedDirs : this.SYSTEM_DIRS;
    for (const dir of dirsToMount) {
      try {
        builder.ro-bind(dir, dir);
      } catch {
        // 目录不存在，跳过
      }
    }

    // 沙箱目录本身绑定
    builder.bind(sandboxPath, sandboxPath);

    // 创建临时文件系统
    builder.tmpfs('/tmp', config.tmpfsSize || '256m');
    builder.tmpfs('/run', '32m');

    // 设备节点
    builder.devNull();
    builder.dev();

    // 资源限制
    builder.limitAs('512m')        // 虚拟内存 512MB
           .limitNofile(1024);     // 最多 1024 个文件描述符

    return builder.build(['bash', '-c', 'exec "$@"', '_']);
  }

  /**
   * 创建标准隔离（允许部分网络）
   */
  static buildStandardSandbox(config: BubblewrapConfig): string[] {
    const { sandboxPath, readonly, allowedDirs } = config;
    const builder = new BubblewrapBuilder(sandboxPath, readonly);

    builder.unshareUser()
           .unsharePid()
           .unshareIpc()
           .unshareCgroup()
           .disallowNewPrivs();

    builder.setHostname(`agent-sandbox-${Date.now()}`)
           .setEnv('HOME', sandboxPath)
           .setEnv('USER', 'agent')
           .setEnv('PATH', '/usr/local/bin:/usr/bin:/bin:/usr/local/sbin:/usr/sbin:/sbin')
           .setEnv('TERM', 'xterm-256color');

    builder.chdir(sandboxPath);

    // 挂载系统目录
    const dirsToMount = allowedDirs.length > 0 ? allowedDirs : this.SYSTEM_DIRS;
    for (const dir of dirsToMount) {
      try {
        builder.ro-bind(dir, dir);
      } catch {
        // 目录不存在
      }
    }

    builder.bind(sandboxPath, sandboxPath);
    builder.tmpfs('/tmp', config.tmpfsSize || '256m');
    builder.tmpfs('/run', '32m');
    builder.devNull();
    builder.dev();

    builder.limitAs('1g')
           .limitNofile(2048);

    return builder.build(['bash', '-c', 'exec "$@"', '_']);
  }

  /**
   * 创建基础隔离（仅文件系统隔离）
   */
  static buildBasicSandbox(config: BubblewrapConfig): string[] {
    const { sandboxPath, allowedDirs } = config;
    const builder = new BubblewrapBuilder(sandboxPath, false);

    builder.setEnv('HOME', sandboxPath)
           .setEnv('PATH', '/usr/local/bin:/usr/bin:/bin')
           .setEnv('TERM', 'xterm-256color');

    builder.chdir(sandboxPath);

    // 允许的目录
    const dirsToMount = allowedDirs.length > 0 ? allowedDirs : this.SYSTEM_DIRS;
    for (const dir of dirsToMount) {
      try {
        builder.bind(dir, dir);
      } catch {
        // 目录不存在
      }
    }

    builder.tmpfs('/tmp', config.tmpfsSize || '128m');

    return builder.build(['bash', '-c', 'exec "$@"', '_']);
  }

  /**
   * 执行命令（带 bwrap 隔离）
   */
  static async exec(
    command: string,
    sandboxPath: string,
    level: BubblewrapLevel = 'standard',
    options: {
      allowedDirs?: string[];
      networkEnabled?: boolean;
      tmpfsSize?: string;
      timeoutMs?: number;
    } = {}
  ): Promise<{ stdout: string; stderr: string; exitCode: number }> {
    const config: BubblewrapConfig = {
      level,
      sandboxPath,
      readonly: level === 'strict',
      allowedDirs: options.allowedDirs || [],
      networkEnabled: options.networkEnabled ?? (level !== 'strict'),
      tmpfsSize: options.tmpfsSize,
    };

    let cmdParts: string[];
    switch (level) {
      case 'strict':
        cmdParts = this.buildStrictSandbox(config);
        break;
      case 'basic':
        cmdParts = this.buildBasicSandbox(config);
        break;
      default:
        cmdParts = this.buildStandardSandbox(config);
    }

    // 将完整命令添加到 bwrap 链的末尾
    cmdParts.push('sh', '-c', command);

    try {
      const result = await $`${cmdParts.join(' ')}`.cwd(sandboxPath)
        .timeout(options.timeoutMs || 60000);

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
}
