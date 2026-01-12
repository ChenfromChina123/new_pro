我将通过以下方案实现多用户隔离的创作型 Agent 集成：

### 1. 核心代码改造 (Python)
为了支持“每个用户有独立工作目录”且不干扰全局进程，我将修改 `xiaochen_agent_v2` 的核心逻辑，使其支持**配置化工作目录**，而非硬编码依赖 `os.getcwd()`。

*   **`core/config.py`**: 增加 `work_dir` 配置项。
*   **`core/agent.py`**: 在初始化 `Tools` 时传入当前 Agent 实例的 `work_dir`。
*   **`tools/executor.py`**: 将所有的 `os.getcwd()` 替换为 `self.work_dir`，并在文件操作前增加路径安全检查（Sandbox Check），防止用户越权访问其他目录。

### 2. Web 适配层 (`api_server.py`)
*   **多用户管理**: 当 WebSocket 连接 (`/ws/agent?userId=xxx`) 建立时：
    1.  为该用户创建一个专属的 `workspaces/<user_id>` 目录。
    2.  实例化一个独立的 `Agent`，并将其 `work_dir` 指向该目录。
    3.  启动专用线程运行 Agent，并绑定线程级 I/O 队列。
*   **角色定制**: 创建 `WebAgent` 类继承自 `Agent`，重写 `getContextOfSystem` 方法，加载专门为“作品生成”定制的 System Prompt，强调创造性输出。

### 3. 前端集成 (Vue)
*   **`AgentView.vue`**: 使用 `xterm.js` 实现全功能终端。
*   **交互逻辑**: 连接 WebSocket 时自动携带当前用户身份，确保进入独立的工作空间。

### 4. 执行步骤
1.  **安装依赖**: `fastapi`, `uvicorn`, `websockets`, `xterm`。
2.  **修改核心**: 改造 Agent 文件系统逻辑以支持沙箱模式。
3.  **创建服务**: 实现 `api_server.py` 和 `web_adapter.py`。
4.  **开发前端**: 实现 Vue 终端界面。
5.  **配置环境**: 提供启动脚本。

此方案实现了真正的**多租户隔离**（文件系统+会话+I/O），并将 Agent 转型为专属的创作助手。
