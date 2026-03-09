# SSH 终端多窗口模式实施计划

## 任务概述
取消当前的持久化连接功能，改为支持多窗口终端模式，允许用户同时打开多个独立的终端窗口。

## 当前问题分析

### 现有持久化功能
1. **后端**：
   - `SSHWebSocketHandler.java` 中使用了多个静态 Map 保存连接状态
   - 使用 `userServerKey` (userId_serverId) 作为键保存终端输出
   - WebSocket 关闭时不断开 SSH 连接
   - 刷新页面后恢复连接和输出历史

2. **前端**：
   - `ServerTerminalView.vue` 使用 localStorage 保存连接状态
   - 页面加载时自动恢复连接
   - 从后端 API 获取历史输出

### 需要修改的内容
1. 移除所有持久化相关的代码
2. 改为标准的 WebSocket 生命周期管理
3. 添加多窗口支持

## 实施步骤

### 第一阶段：后端修改

#### 1.1 简化 SSHWebSocketHandler.java
- **移除持久化相关的 Map**：
  - 移除 `userServerSessions` Map
  - 移除 `sessionToUserServer` Map
  - 移除 `terminalOutputs` Map
  
- **修改连接处理逻辑**：
  - 移除 `handleConnect` 中的连接复用检查
  - 每次连接都创建新的 SSH 会话
  - WebSocket 关闭时立即断开 SSH 连接

- **修改输出处理**：
  - 移除 `appendTerminalOutput` 方法
  - 移除 `getTerminalOutput` 方法
  - 移除 `getSessionIdByUserServer` 方法
  - 输出直接发送到 WebSocket，不保存历史

- **修改断开连接逻辑**：
  - `afterConnectionClosed` 中调用 `handleDisconnect`
  - 清理所有相关资源

#### 1.2 移除 REST API
- **ServerTerminalController.java**：
  - 移除 `/servers/{serverId}/output` API
  - 保留其他必要的 API

### 第二阶段：前端修改

#### 2.1 移除持久化功能
- **ServerTerminalView.vue**：
  - 移除 localStorage 相关代码
  - 移除 `loadConnections` 方法
  - 移除 `saveConnection` 方法
  - 移除 `removeConnection` 方法
  - 移除 `fetchTerminalOutput` 方法
  - 移除 `onMounted` 中的自动恢复逻辑

#### 2.2 添加多窗口支持

**UI 改造**：
- 在服务器列表中添加"新开窗口"按钮
- 使用 `window.open()` 打开新窗口
- 每个窗口独立的终端会话

**实现方案**：
```javascript
// 打开新终端窗口
const openNewTerminal = (serverId) => {
  const url = `/terminal/${serverId}`
  window.open(url, '_blank', 'width=1200,height=800')
}
```

**路由配置**：
- 添加新的路由 `/terminal/:serverId`
- 创建独立的终端页面组件
- 每个窗口独立管理 WebSocket 连接

### 第三阶段：清理和优化

#### 3.1 代码清理
- 移除所有未使用的导入
- 移除调试日志
- 优化代码结构

#### 3.2 测试验证
- 测试单窗口连接和断开
- 测试多窗口同时连接
- 测试窗口关闭后资源清理
- 测试命令输入和输出显示

## 技术细节

### 后端关键修改

**SSHWebSocketHandler.java 简化后的结构**：
```java
@Component
public class SSHWebSocketHandler extends TextWebSocketHandler {
    private static ServerConnectionRepository serverConnectionRepository;
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final Map<String, com.jcraft.jsch.Session> jschSessions = new ConcurrentHashMap<>();
    private static final Map<String, ChannelShell> channels = new ConcurrentHashMap<>();
    private static final Map<String, OutputStream> outputStreams = new ConcurrentHashMap<>();
    
    // 连接建立 -> 创建 SSH 会话
    // 接收消息 -> 处理命令
    // 连接关闭 -> 断开 SSH
}
```

### 前端关键修改

**多窗口实现**：
1. **主页面**：显示服务器列表，每个服务器有"连接"和"新开窗口"按钮
2. **独立终端页面**：`/terminal/:serverId` 路由，独立管理 WebSocket 连接
3. **窗口通信**：不需要窗口间通信，每个窗口独立运行

### 资源管理

**确保资源正确释放**：
- WebSocket 关闭时立即断开 SSH 连接
- 清理所有相关的 Map 条目
- 停止输出读取线程

## 预期效果

### 用户体验
1. **主页面**：
   - 显示服务器列表
   - 点击"连接"在当前页面打开终端
   - 点击"新开窗口"打开独立终端窗口

2. **终端窗口**：
   - 独立的终端会话
   - 可以同时打开多个窗口连接同一服务器
   - 关闭窗口自动断开连接

3. **无持久化**：
   - 刷新页面连接断开
   - 每次都是全新的会话
   - 简单直接，无状态管理

## 风险和注意事项

1. **资源泄漏风险**：
   - 确保所有资源在连接关闭时正确释放
   - 添加超时机制防止僵尸连接

2. **多窗口并发**：
   - 多个窗口可以同时连接同一服务器
   - 每个窗口独立的 SSH 会话
   - 不会相互干扰

3. **浏览器弹窗拦截**：
   - 需要用户允许弹窗
   - 提供友好的提示信息

## 实施顺序

1. ✅ 后端：移除持久化相关代码
2. ✅ 后端：简化连接管理逻辑
3. ✅ 前端：移除 localStorage 和自动恢复
4. ✅ 前端：添加多窗口 UI
5. ✅ 前端：创建独立终端页面
6. ✅ 测试：验证功能完整性
7. ✅ 清理：移除调试代码和日志

## 预计工作量
- 后端修改：约 30 分钟
- 前端修改：约 45 分钟
- 测试验证：约 15 分钟
- 总计：约 1.5 小时
