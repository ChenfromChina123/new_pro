# 服务器终端功能增强与 SFTP 集成开发计划

本文档详细说明了服务器终端（Server Terminal）的功能增强计划，包括会话持久化、多窗口支持以及与 SFTP 文件管理器的集成。

## 1. 概述 (Overview)

本次开发旨在提升服务器终端的用户体验和工作效率，主要包含以下三个核心功能：
1.  **终端持久化 (Persistence):** 页面刷新后，能够自动恢复之前打开的终端窗口和布局，保持工作上下文。
2.  **多终端窗口 (Multi-terminal Windows):** 支持用户在同一界面通过标签页（Tab）的方式同时打开和管理多个终端会话。
3.  **SFTP 集成 (SFTP Integration):** 允许用户在 SFTP 文件管理器中直接打开当前目录对应的终端窗口，实现文件管理与命令行操作的无缝切换。

## 2. 详细需求与设计 (Detailed Requirements & Design)

### 2.1 服务器终端持久化 (Server Terminal Persistence)

**当前状态:**
-   虽然数据库中存储了服务器列表，但浏览器端的终端会话状态（如打开了哪个服务器、当前标签页等）在页面刷新后会丢失。

**解决方案:**
-   **会话状态存储:** 使用浏览器的 `localStorage` 来实时保存当前打开的终端标签页列表（包括 Server ID、标签名称、激活状态等）。
-   **自动恢复:** 当 `ServerTerminalView` 页面加载时，自动读取 `localStorage` 中的配置，并根据保存的信息重新创建标签页。
-   **连接恢复:**
    -   注意：由于 SSH 协议的特性，页面刷新会导致底层的 WebSocket 连接断开。
    -   系统将自动为恢复的标签页重新建立 WebSocket 连接。
    -   *注：服务器端的 Shell 临时变量或运行中的前台进程可能会丢失（除非使用了 screen/tmux 等工具），但前端的窗口布局和连接通道将自动恢复。*

### 2.2 多终端窗口 (Multi-terminal Windows)

**当前状态:**
-   目前仅支持单实例终端，切换服务器会覆盖当前终端界面。

**解决方案:**
-   **标签页系统 (Tab System):** 在 `ServerTerminalView.vue` 中引入标签页界面。
-   **组件重构:** 将核心终端逻辑（WebSocket 连接、Xterm.js 渲染、输入处理）提取为独立的子组件 `TerminalTab.vue`。
-   **状态管理:**
    -   支持通过“+”按钮或从左侧服务器列表点击来添加新标签页。
    -   支持关闭标签页。
    -   支持在不同标签页之间快速切换。
    -   支持修改标签页标题（可选）。

### 2.3 SFTP "在当前目录打开终端" (SFTP Integration)

**当前状态:**
-   SFTP 文件管理器 (`SFTPManagerView.vue`) 与服务器终端是两个独立的模块，无法直接交互。

**解决方案:**
-   **右键菜单扩展:** 在 `FilePanel.vue` 的文件/目录右键菜单中添加 “在终端打开 (Open Terminal Here)” 选项。
-   **导航与传参:**
    -   点击该选项后，跳转至 `ServerTerminalView` 页面。
    -   通过 URL 参数传递上下文信息：`serverId` (服务器ID) 和 `path` (当前路径)。例如：`/terminal?serverId=1&path=/var/www/html`。
-   **终端自动执行:**
    -   `ServerTerminalView` 在初始化时检查 URL 参数。
    -   如果存在参数，则自动为指定服务器打开一个新的终端标签页。
    -   连接成功建立后，自动发送 `cd <path>` 命令，切换到目标目录。

## 3. 实施步骤 (Implementation Steps)

### 第一阶段：前端重构 (Vue)

1.  **创建 `TerminalTab.vue` 组件:**
    -   将原 `ServerTerminalView.vue` 中的终端核心代码（Xterm 实例、WebSocket 通信、自适应大小等）剥离出来。
    -   定义 Props: `serverId` (必填), `initialPath` (可选，用于 SFTP 跳转)。
    -   定义 Events: `close` (关闭标签), `ready` (连接就绪)。

2.  **改造 `ServerTerminalView.vue`:**
    -   引入标签页 UI 结构（Tab Bar + Tab Content）。
    -   维护一个 `tabs` 数组状态，例如：`[{ id: 'tab-1', serverId: 1, title: '生产服务器' }]`。
    -   实现 `localStorage` 的读写逻辑，在 `tabs` 变化时同步保存，在 `onMounted` 时读取恢复。
    -   处理 URL 查询参数，实现从 SFTP 跳转过来的自动打开逻辑。

3.  **更新 `FilePanel.vue` (SFTP):**
    -   修改右键菜单数据结构，增加“在终端打开”项。
    -   实现跳转逻辑：使用 `router.push({ name: 'ServerTerminal', query: { ... } })`。

### 第二阶段：后端验证 (Spring Boot)

-   **并发连接检查:** 确认后端的 `SSHWebSocketHandler` 能正确处理同一用户的多个并发 WebSocket 连接（基于 Session ID 区分），确保多标签页同时工作互不干扰。
-   *预期不需要大规模修改后端代码。*

## 4. 用户验证 (User Verification)

请确认以上设计是否满足您的需求，特别是：
1.  **持久化范围:** 确认只需要恢复窗口布局和连接，不需要恢复服务器端 Shell 的历史上下文（这通常由服务器端工具如 tmux 处理）。
2.  **交互方式:** 确认 SFTP 右键跳转的交互方式符合预期。

确认无误后，我将开始代码编写。
