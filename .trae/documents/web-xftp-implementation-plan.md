# Web Xftp 文件管理器实现计划

## 一、项目概述

基于现有的 Web 终端系统，扩展实现一个类似 Xftp 的 Web 文件管理器，打造完整的云端运维"全家桶"。

### 核心目标
- 远程服务器文件浏览与管理
- 大文件流式传输（避免 OOM）
- 海量文件列表高性能渲染
- SFTP 连接复用与池化管理
- 实时传输进度显示

---

## 二、技术架构

### 2.1 后端技术栈 (Spring Boot)

| 组件 | 技术选型 | 说明 |
|------|---------|------|
| SFTP 客户端 | **SSHJ** (替代 JSch) | API 现代，支持新加密算法，社区活跃 |
| 连接池 | Apache Commons Pool2 | 复用 SFTP 连接，避免频繁握手 |
| 文件传输 | 原生流式处理 | InputStream → OutputStream 直接对接 |
| 进度推送 | WebSocket | 复用现有 WebSocket 基础设施 |

### 2.2 前端技术栈 (Vue 3)

| 组件 | 技术选型 | 说明 |
|------|---------|------|
| 窗格布局 | **Splitpanes** | 可拖拽分割，实现 Xftp 风格布局 |
| 文件列表 | **vxe-table** | 虚拟滚动，支持海量文件渲染 |
| 文件上传 | **Uppy** | 分片上传、断点续传、拖拽文件夹 |
| 文件图标 | 自定义 SVG 图标集 | VS Code 风格文件类型图标 |

### 2.3 通信协议设计

```
┌─────────────────────────────────────────────────────────────┐
│                     前端 (Vue 3)                            │
├─────────────────────────────────────────────────────────────┤
│  HTTP REST API          │  WebSocket (进度推送)             │
│  - 文件列表/目录操作      │  - 上传/下载进度                  │
│  - 文件上传/下载         │  - 传输状态变更                   │
├─────────────────────────────────────────────────────────────┤
│                     后端 (Spring Boot)                      │
├─────────────────────────────────────────────────────────────┤
│  SFTP 连接池 (Commons Pool2)                                │
│  - 连接复用                                                │
│  - 自动回收                                                │
├─────────────────────────────────────────────────────────────┤
│  SSHJ SFTP Client                                          │
│  - 流式文件传输                                            │
│  - 目录遍历                                                │
└─────────────────────────────────────────────────────────────┘
```

---

## 三、实现步骤

### 阶段一：后端基础设施 (预计 3-4 小时)

#### 1.1 添加 Maven 依赖
```xml
<!-- SSHJ - SFTP 客户端 -->
<dependency>
    <groupId>com.hierynomus</groupId>
    <artifactId>sshj</artifactId>
    <version>0.38.0</version>
</dependency>

<!-- Apache Commons Pool2 - 连接池 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

#### 1.2 创建 SFTP 连接池
- 文件：`SFTPClientFactory.java` - 创建 SFTP 客户端
- 文件：`SFTPClientPool.java` - 连接池管理
- 文件：`SFTPClientPoolConfig.java` - 连接池配置

#### 1.3 创建 SFTP 服务层
- 文件：`SFTPService.java` - SFTP 操作服务接口
- 文件：`SFTPServiceImpl.java` - SFTP 操作实现
  - `listFiles(path)` - 列出目录文件
  - `uploadFile(path, inputStream)` - 上传文件
  - `downloadFile(path, outputStream)` - 下载文件
  - `deleteFile(path)` - 删除文件
  - `renameFile(oldPath, newPath)` - 重命名
  - `mkdir(path)` - 创建目录
  - `exists(path)` - 检查路径是否存在

#### 1.4 创建 REST 控制器
- 文件：`SFTPController.java`
  - `GET /api/sftp/{serverId}/files` - 获取文件列表
  - `POST /api/sftp/{serverId}/upload` - 上传文件
  - `GET /api/sftp/{serverId}/download` - 下载文件
  - `DELETE /api/sftp/{serverId}/file` - 删除文件
  - `PUT /api/sftp/{serverId}/rename` - 重命名
  - `POST /api/sftp/{serverId}/mkdir` - 创建目录

### 阶段二：WebSocket 进度推送 (预计 1-2 小时)

#### 2.1 创建 SFTP WebSocket 处理器
- 文件：`SFTPWebSocketHandler.java`
  - 处理文件传输进度推送
  - 支持多任务并发传输

#### 2.2 进度消息协议
```json
{
  "type": "upload_progress",
  "taskId": "uuid",
  "fileName": "example.tar.gz",
  "totalSize": 104857600,
  "transferredSize": 52428800,
  "progress": 50,
  "speed": "5.2 MB/s",
  "status": "transferring"
}
```

### 阶段三：前端基础组件 (预计 4-5 小时)

#### 3.1 安装前端依赖
```bash
npm install splitpanes vxe-table uppy @uppy/vue
```

#### 3.2 创建文件管理器视图
- 文件：`SFTPManagerView.vue`
  - 左侧：本地文件面板
  - 右侧：远程服务器文件面板
  - 底部：传输队列面板

#### 3.3 创建文件列表组件
- 文件：`FileTable.vue`
  - 使用 vxe-table 虚拟滚动
  - 支持排序、筛选、多选
  - 文件类型图标显示

#### 3.4 创建传输队列组件
- 文件：`TransferQueue.vue`
  - 显示上传/下载任务
  - 实时进度条
  - 暂停/继续/取消操作

### 阶段四：核心功能实现 (预计 5-6 小时)

#### 4.1 文件浏览功能
- 目录导航（面包屑）
- 文件列表加载
- 文件排序与筛选
- 文件搜索

#### 4.2 文件上传功能
- 拖拽上传
- 文件夹上传
- 分片上传
- 断点续传
- 上传进度显示

#### 4.3 文件下载功能
- 单文件下载
- 多文件打包下载
- 文件夹递归下载
- 下载进度显示

#### 4.4 文件操作功能
- 新建文件夹
- 重命名
- 删除（支持批量）
- 复制/移动（可选）

### 阶段五：优化与完善 (预计 2-3 小时)

#### 5.1 性能优化
- 文件列表虚拟滚动优化
- 大文件分块传输
- 连接池参数调优

#### 5.2 用户体验优化
- 文件预览功能（文本、图片）
- 右键菜单
- 快捷键支持
- 移动端适配

#### 5.3 错误处理
- 网络断开重连
- 传输失败重试
- 权限错误提示

---

## 四、文件结构规划

### 4.1 后端新增文件

```
aispring/src/main/java/com/aispring/
├── sftp/
│   ├── config/
│   │   ├── SFTPClientPoolConfig.java      # 连接池配置
│   │   └── SFTPConfig.java                # SFTP 配置类
│   ├── pool/
│   │   ├── SFTPClientFactory.java         # SFTP 客户端工厂（含保活验证）
│   │   └── SFTPClientPool.java            # SFTP 连接池
│   ├── service/
│   │   ├── SFTPService.java               # SFTP 服务接口
│   │   ├── impl/SFTPServiceImpl.java      # SFTP 服务实现
│   │   └── TransferProgressService.java   # 传输进度服务（含节流，新增）
│   ├── dto/
│   │   ├── FileInfo.java                  # 文件信息 DTO
│   │   └── TransferProgress.java          # 传输进度 DTO
│   └── controller/
│       └── SFTPController.java            # SFTP REST 控制器
├── config/
│   └── SFTPWebSocketHandler.java          # SFTP WebSocket 处理器
```

### 4.2 前端新增文件

```
vue-app/src/
├── views/
│   └── SFTPManagerView.vue                # SFTP 文件管理器主视图
├── components/sftp/
│   ├── FilePanel.vue                      # 文件面板组件
│   ├── FileTable.vue                      # 文件列表表格
│   ├── FileBreadcrumb.vue                 # 面包屑导航
│   ├── TransferQueue.vue                  # 传输队列
│   ├── TransferItem.vue                   # 传输任务项
│   ├── FileIcon.vue                       # 文件图标组件
│   └── StatusBar.vue                      # 状态栏组件（新增）
├── stores/
│   └── sftp.js                            # SFTP 状态管理
├── services/
│   └── sftpService.js                     # SFTP API 服务
├── composables/
│   └── useDragTransfer.js                 # 跨面板拖拽逻辑（新增）
└── utils/
    └── fileIcons.js                       # 文件图标映射
```

---

## 五、API 接口设计

### 5.1 文件操作接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/sftp/{serverId}/files` | 获取目录文件列表 |
| GET | `/api/sftp/{serverId}/download` | 下载文件 |
| POST | `/api/sftp/{serverId}/upload` | 上传文件 |
| POST | `/api/sftp/{serverId}/mkdir` | 创建目录 |
| PUT | `/api/sftp/{serverId}/rename` | 重命名文件/目录 |
| DELETE | `/api/sftp/{serverId}/file` | 删除文件/目录 |
| GET | `/api/sftp/{serverId}/exists` | 检查路径是否存在 |

### 5.2 请求/响应示例

**获取文件列表**
```http
GET /api/sftp/1/files?path=/home/user

Response:
{
  "code": 200,
  "data": {
    "path": "/home/user",
    "files": [
      {
        "name": "documents",
        "path": "/home/user/documents",
        "isDirectory": true,
        "size": 4096,
        "modifiedTime": "2024-01-15T10:30:00",
        "permissions": "drwxr-xr-x"
      },
      {
        "name": "config.json",
        "path": "/home/user/config.json",
        "isDirectory": false,
        "size": 1024,
        "modifiedTime": "2024-01-14T15:20:00",
        "permissions": "-rw-r--r--"
      }
    ]
  }
}
```

---

## 六、关键技术实现要点

### 6.1 流式文件传输（避免 OOM）

```java
// 下载 - 直接流式传输
@GetMapping("/download")
public void downloadFile(@PathVariable Long serverId, 
                         @RequestParam String path,
                         HttpServletResponse response) {
    SFTPClient sftp = sftpPool.borrowObject(serverId);
    try {
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", 
            "attachment; filename=" + getFileName(path));
        
        try (InputStream is = sftp.newSCPFileTransfer()
                                  .downloadStream(path)) {
            IOUtils.copy(is, response.getOutputStream());
        }
    } finally {
        sftpPool.returnObject(serverId, sftp);
    }
}
```

### 6.2 连接池配置与保活策略

```java
@Configuration
public class SFTPClientPoolConfig {
    
    @Bean
    public GenericObjectPool<SFTPClient> sftpPool() {
        GenericObjectPoolConfig<SFTPClient> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(10);           // 最大连接数
        config.setMaxIdle(5);             // 最大空闲连接
        config.setMinIdle(2);             // 最小空闲连接
        config.setMaxWaitMillis(30000);   // 最大等待时间
        config.setTestOnBorrow(true);     // 借出时测试
        config.setTestOnReturn(false);    // 归还时不测试
        config.setTestWhileIdle(true);    // 空闲时测试
        config.setTimeBetweenEvictionRunsMillis(60000); // 空闲检测间隔
        
        return new GenericObjectPool<>(new SFTPClientFactory(), config);
    }
}
```

**连接保活策略：**

```java
public class SFTPClientFactory extends BasePooledObjectFactory<SFTPClient> {
    
    @Override
    public SFTPClient create(ServerConnection server) throws Exception {
        SSHClient ssh = new SSHClient();
        ssh.addHostKeyVerifier((hostname, port, key) -> true);
        ssh.connect(server.getHost(), server.getPort());
        ssh.authPassword(server.getUsername(), server.getPassword());
        
        // 关键：设置 Keep-Alive 防止连接被中间网络设备回收
        ssh.getConnection().setKeepAliveInterval(30); // 每 30 秒发送心跳
        
        return ssh.newSFTPClient();
    }
    
    @Override
    public boolean validateObject(PooledObject<SFTPClient> p) {
        try {
            // 轻量级验证：执行 stat("/") 检查连接是否存活
            p.getObject().stat("/");
            return true;
        } catch (Exception e) {
            log.warn("SFTP 连接验证失败，将被销毁: {}", e.getMessage());
            return false;
        }
    }
}
```

### 6.3 进度推送的背压控制

**问题：** 高并发上传大文件时，每秒产生的进度更新包可能成百上千，频繁的 WebSocket 消息会阻塞 UI 渲染线程。

**后端节流策略：**

```java
@Service
public class TransferProgressService {
    
    // 每个任务的上次推送时间
    private final Map<String, Long> lastPushTime = new ConcurrentHashMap<>();
    
    // 节流间隔：200ms
    private static final long THROTTLE_INTERVAL_MS = 200;
    
    /**
     * 发送进度更新（带节流）
     * @param taskId 任务ID
     * @param progress 进度信息
     */
    public void sendProgressUpdate(String taskId, TransferProgress progress) {
        long now = System.currentTimeMillis();
        Long lastTime = lastPushTime.get(taskId);
        
        // 节流：距离上次推送不足 200ms，跳过（除非是完成/失败状态）
        if (lastTime != null && (now - lastTime) < THROTTLE_INTERVAL_MS 
            && progress.getStatus() == TransferStatus.TRANSFERRING) {
            return;
        }
        
        lastPushTime.put(taskId, now);
        webSocketHandler.sendProgress(taskId, progress);
    }
}
```

**前端平滑渲染：**

```javascript
// 使用 requestAnimationFrame 平滑进度条
const smoothProgress = ref(0)
const targetProgress = ref(0)

watch(() => props.progress, (newVal) => {
  targetProgress.value = newVal
  requestAnimationFrame(animateProgress)
})

function animateProgress() {
  if (smoothProgress.value < targetProgress.value) {
    smoothProgress.value += (targetProgress.value - smoothProgress.value) * 0.1
    if (smoothProgress.value < targetProgress.value) {
      requestAnimationFrame(animateProgress)
    }
  }
}
```

### 6.4 虚拟滚动文件列表

```vue
<template>
  <vxe-table
    :data="files"
    :scroll-y="{ enabled: true, gt: 100 }"
    height="100%"
  >
    <vxe-column type="checkbox" width="50" />
    <vxe-column field="name" title="名称">
      <template #default="{ row }">
        <div class="file-name">
          <FileIcon :type="row.isDirectory ? 'folder' : getFileType(row.name)" />
          <span>{{ row.name }}</span>
        </div>
      </template>
    </vxe-column>
    <vxe-column field="size" title="大小" width="120" />
    <vxe-column field="modifiedTime" title="修改时间" width="180" />
    <vxe-column field="permissions" title="权限" width="120" />
  </vxe-table>
</template>
```

### 6.5 Xftp 风格的视觉细节

**跨面板拖拽上传：**

```vue
<template>
  <div class="file-panel"
       @dragstart="handleDragStart"
       @dragover.prevent
       @drop="handleDrop">
    <!-- 文件列表 -->
  </div>
</template>

<script setup>
/**
 * 处理拖拽开始事件
 * @param {DragEvent} event - 拖拽事件对象
 */
const handleDragStart = (event) => {
  const selectedFiles = getSelectedFiles()
  event.dataTransfer.setData('application/json', JSON.stringify({
    source: 'local',  // 或 'remote'
    files: selectedFiles
  }))
  event.dataTransfer.effectAllowed = 'copy'
}

/**
 * 处理放置事件
 * @param {DragEvent} event - 放置事件对象
 */
const handleDrop = (event) => {
  const data = JSON.parse(event.dataTransfer.getData('application/json'))
  if (data.source === 'local' && props.panelType === 'remote') {
    // 从本地上传到远程
    startUpload(data.files, currentPath.value)
  } else if (data.source === 'remote' && props.panelType === 'local') {
    // 从远程下载到本地
    startDownload(data.files, currentPath.value)
  }
}
</script>
```

**状态栏组件：**

```vue
<template>
  <div class="status-bar">
    <div class="status-left">
      <span class="connection-status" :class="connected ? 'connected' : 'disconnected'">
        ● {{ connected ? '已连接' : '未连接' }}
      </span>
      <span v-if="serverInfo">{{ serverInfo.host }}:{{ serverInfo.port }}</span>
    </div>
    <div class="status-center">
      <span v-if="selectedCount > 0">
        已选择 {{ selectedCount }} 项，共 {{ formatSize(selectedSize) }}
      </span>
    </div>
    <div class="status-right">
      <span v-if="transferCount > 0">
        传输中: {{ transferCount }} 个任务
      </span>
      <span>{{ currentTime }}</span>
    </div>
  </div>
</template>

<style scoped>
.status-bar {
  height: 24px;
  background: var(--status-bar-bg);
  border-top: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  font-size: 12px;
  color: var(--text-dim);
}

.connection-status.connected {
  color: var(--success);
}

.connection-status.disconnected {
  color: var(--danger);
}
</style>
```

---

## 七、风险与应对

| 风险 | 影响 | 应对措施 |
|------|------|---------|
| 大文件传输内存溢出 | 高 | 严格使用流式传输，禁止全量加载 |
| 连接池耗尽 | 中 | 设置合理连接数上限，添加等待超时 |
| 网络中断 | 中 | 实现断点续传，自动重连机制 |
| 海量文件列表卡顿 | 中 | 使用虚拟滚动，分页加载 |
| 权限不足操作失败 | 低 | 友好错误提示，记录日志 |
| **SFTP 连接闲置断开** | 高 | validateObject 执行 stat("/") 验证 + Keep-Alive 心跳 |
| **进度推送阻塞 UI** | 中 | 后端 200ms 节流 + 前端 requestAnimationFrame 平滑渲染 |
| **跨面板拖拽识别** | 低 | dragstart/drop 事件捕获源路径和目标路径 |

---

## 八、实施优先级

### P0 - 核心功能（必须实现）
1. SFTP 连接池搭建（含保活验证：validateObject + Keep-Alive）
2. 文件列表浏览
3. 文件上传/下载（基础版）
4. 传输进度显示（含 200ms 节流）

### P1 - 重要功能
1. 文件夹上传/下载
2. 断点续传
3. 文件操作（重命名、删除、新建文件夹）
4. 批量操作
5. 状态栏（连接状态、选中统计、传输任务数）

### P2 - 增强功能
1. 文件预览
2. 跨面板拖拽上传/下载
3. 右键菜单
4. 快捷键

---

## 九、预计工作量

| 阶段 | 预计时间 |
|------|---------|
| 后端基础设施（含连接池保活） | 4-5 小时 |
| WebSocket 进度推送（含节流） | 2-3 小时 |
| 前端基础组件（含状态栏） | 5-6 小时 |
| 核心功能实现 | 5-6 小时 |
| 优化与完善（含跨面板拖拽） | 3-4 小时 |
| **总计** | **19-24 小时** |

---

## 十、后续扩展方向

1. **多服务器文件同步** - 服务器间文件复制
2. **文件编辑器** - 在线编辑远程文件
3. **文件搜索** - 全文搜索远程文件
4. **收藏夹** - 常用路径快速访问
5. **历史记录** - 操作日志与回滚
