# Agent 沙箱隔离系统 (Linux)

本文档说明了 Linux 环境下 Agent 执行层的真实安全隔离实现。

## 架构概览

```
┌─────────────────┐      ┌──────────────────┐      ┌─────────────────┐
│   前端 (Vue)    │ ──── │  aispring (Java) │ ──── │ agent-executor  │
│                 │ SSE  │   Controller     │ HTTP │   (TypeScript)   │
│                 │◄──── │   Service       │ SSE  │   ReActEngine   │
└─────────────────┘      └──────────────────┘      └─────────────────┘
                                                            │
                                                    ┌───────┴───────┐
                                                    │   隔离层       │
                                                    ├───────────────┤
                                                    │ bwrap (容器)  │
                                                    │ cgroups (资源)│
                                                    │ Git (快照)    │
                                                    └───────────────┘
```

## 隔离技术栈

### 1. Bubblewrap (bwrap) - 容器级隔离

**文件**: `src/sandbox/Bubblewrap.ts`

Bubblewrap 是一个 Linux 命名空间隔离工具，提供：

- **用户命名空间 (user namespace)**: 隔离用户 ID/GID 映射
- **PID 命名空间**: 隔离进程树
- **网络命名空间 (可选)**: 完全网络隔离
- **挂载命名空间**: 文件系统视图隔离
- **IPC 命名空间**: 进程间通信隔离
- **Cgroup 命名空间**: 资源控制组隔离

#### 隔离级别

| 级别 | 描述 | 网络 | 内存限制 |
|------|------|------|---------|
| `strict` | 最高安全 | 无网络 | 256MB |
| `standard` | 平衡 | 无网络 | 512MB |
| `basic` | 最小隔离 | 有网络 | 128MB |

#### bwrap 命令示例

```bash
# strict 级别
bwrap \
  --unshare-user \
  --unshare-pid \
  --unshare-ipc \
  --unshare-net \
  --unshare-cgroup \
  --disallow-new-privs \
  --hostname agent-sandbox-123 \
  --setenv HOME /tmp/agent-sandbox \
  --chdir /tmp/agent-sandbox \
  --ro-bind /bin /bin \
  --ro-bind /usr/bin /usr/bin \
  --tmpfs /tmp \
  --dev /dev \
  --limit-as 256m \
  -- \
  bash -c 'exec "$@"'
```

### 2. cgroups v2 - 资源限制

**文件**: `src/sandbox/CgroupManager.ts`

cgroups (Control Groups) v2 提供细粒度资源控制：

#### 资源限制

| 资源 | 参数 | 说明 |
|------|------|------|
| 内存 | `memoryMax` | 最大内存 (字节) |
| CPU | `cpuWeight` | CPU 权重 (1-10000) |
| CPU | `cpuMax` | CPU 时间额度 |
| 进程数 | `pidsMax` | 最大进程数 |
| IO | `ioWeight` | IO 权重 |

#### 预设配置

```typescript
// strict: 严格限制
{
  memoryMax: 256 * 1024 * 1024,      // 256MB
  memorySwapMax: 256 * 1024 * 1024,  // 无交换
  cpuWeight: 100,
  cpuMax: '50000 100000',           // 50% CPU
  pidsMax: 64,
}

// standard: 平衡
{
  memoryMax: 512 * 1024 * 1024,      // 512MB
  memorySwapMax: 1024 * 1024 * 1024, // 1GB 交换
  cpuWeight: 512,
  cpuMax: '100000 100000',           // 100% CPU
  pidsMax: 256,
}

// relaxed: 宽松 (编译等)
{
  memoryMax: 2 * 1024 * 1024 * 1024, // 2GB
  cpuWeight: 2048,
  pidsMax: 1024,
}
```

### 3. Git 快照 - 操作回滚

**文件**: `src/sandbox/SandboxManager.ts`

每个沙箱初始化时创建 Git 仓库，支持：

- `git init`: 初始化仓库
- `git add . && git commit`: 创建快照
- `git reset --hard HEAD^`: 回滚操作

## API 端点

### agent-executor API

| 端点 | 方法 | 描述 |
|------|------|------|
| `/health` | GET | 健康检查 |
| `/sandbox/create` | POST | 创建沙箱 |
| `/execute` | POST | 执行任务 (SSE) |
| `/api/tools/capabilities` | GET | 获取隔离能力 |
| `/api/tools/sandbox/:sessionId/files` | GET | 列出文件 |
| `/api/tools/sandbox/:sessionId/file` | GET/POST | 读/写文件 |
| `/api/tools/sandbox/:sessionId/terminal` | POST | 执行命令 |
| `/api/tools/sandbox/:sessionId/git` | POST | Git 操作 |
| `/api/tools/sandbox/:sessionId/usage` | GET | 资源使用 |
| `/api/tools/sandbox/:sessionId` | DELETE | 删除沙箱 |

### aispring 代理 API

| 端点 | 方法 | 描述 |
|------|------|------|
| `/api/agent/sandbox/capabilities` | GET | 获取系统能力 |
| `/api/agent/sandbox/:sessionId/files` | GET | 列出文件 |
| `/api/agent/sandbox/:sessionId/file` | GET/POST | 读/写文件 |
| `/api/agent/sandbox/:sessionId/terminal` | POST | 执行命令 |
| `/api/agent/sandbox/:sessionId/git` | POST | Git 操作 |
| `/api/agent/sandbox/:sessionId/usage` | GET | 资源使用 |

## 前端使用示例

```javascript
import { listSandboxFiles, executeSandboxTerminal, getCapabilities } from '@/services/agentService'

// 检查系统能力
const caps = await getCapabilities()
console.log(caps.data)

// 列出沙箱文件
const files = await listSandboxFiles('session-123', '.')
console.log(files.data)

// 执行终端命令
const result = await executeSandboxTerminal('session-123', 'ls -la', 30000)
console.log(result.data)
```

## 安全特性

### 路径限制
- 禁止 `..` 路径遍历
- 禁止访问 `/etc`, `/root` 等系统目录
- 所有操作限制在沙箱目录内

### 命令限制
- 危险命令黑名单: `rm -rf /`, `mkfs`, `dd if=`, fork bomb 等
- 仅允许安全的文件系统操作

### 资源限制
- 内存上限
- CPU 权重
- 最大进程数
- 最大文件描述符数

## 依赖要求

### 系统依赖

```bash
# Ubuntu/Debian
sudo apt-get install bwrap

# CentOS/RHEL
sudo yum install bwrap

# Arch Linux
sudo pacman -S bubblewrap
```

### cgroups 要求

- Linux Kernel 4.5+ (推荐 5.0+)
- cgroups v2 支持 (`/sys/fs/cgroup/unified` 存在)
- Root 权限 (用于创建 cgroups)

## 注意事项

1. **bwrap 需要 CAP_SYS_ADMIN 能力**: 在容器内运行可能需要额外配置
2. **cgroups 需要 root 权限**: 非 root 用户无法创建 cgroups
3. **网络隔离**: strict/standard 模式下网络完全隔离
4. **降级机制**: 如果 bwrap/cgroups 不可用，会降级到基础文件系统隔离

## 故障排查

### bwrap 不可用

```bash
# 检查 bwrap 是否安装
which bwrap

# 检查权限
bwrap --version
```

### cgroups 不可用

```bash
# 检查 cgroups v2
ls /sys/fs/cgroup/unified

# 检查权限 (需要 root)
cat /proc/self/cgroup
```

### 降级日志

```
[INFO] cgroups: Not running as root, resource limits will not be applied
[WARN] bwrap: Not available, using direct execution
```
