# AI 终端执行与上下文管理机制详解

## 1. 概述

AI 终端（AI Terminal）是一个集成了自然语言处理、终端命令执行和上下文感知能力的智能交互系统。它采用**前后端分离**的架构，结合了 Spring Boot 后端、Vue 3 前端以及流式 AI 生成技术（SSE）。其核心目标是让 AI 能够像人类工程师一样，理解用户意图、规划任务、执行命令并根据反馈进行自我修正。

## 2. 系统架构与数据流

整个系统的执行流程是一个闭环的反馈回路：

1.  **用户输入**：用户在前端输入自然语言指令。
2.  **后端处理**：Spring Boot 接收请求，构建 System Prompt，并调用 LLM（如 DeepSeek, Doubao）。
3.  **Agent 循环（后端）**：后端通过 `AiChatService` 维护一个 Agent 循环，负责处理任务状态更新和推理延续。
4.  **流式输出**：AI 的思考过程（Thought）和响应内容（Content）通过 Server-Sent Events (SSE) 实时推送到前端。
5.  **前端执行（Client-Side Execution）**：前端解析 AI 返回的结构化 JSON 指令（如 `execute_command`），调用后端 API 执行实际的 Shell 命令。
6.  **结果反馈**：命令执行结果（stdout/stderr）被前端捕获，并作为新的上下文再次发送给 AI，触发下一轮推理。

---

## 3. 上下文管理机制 (Context Management)

上下文是 AI 理解当前环境和任务状态的关键。系统通过以下四个维度构建完整的上下文：

### 3.1. System Prompt (系统提示词)

System Prompt 是 AI 的"大脑设定"，在 `TerminalController.java` 中动态生成。它包含以下关键部分：

*   **Role (角色设定)**：定义 AI 为 "运行在专用文件夹系统中的智能终端助手"。
*   **Environment Context (环境上下文)**：
    *   **Operating System**: 明确标识为 Windows/PowerShell 环境。
    *   **Storage Root**: 用户的专用隔离存储根目录。
    *   **Current Working Directory (CWD)**: 当前所在的相对路径（如 `/src/main`）。
    *   **User Permission**: 声明沙箱限制（1GB 配额，最大深度 10 层）。
*   **Current Task Context (任务链上下文)**：
    *   动态注入当前正在进行的任务链状态（如 `[进行中] 创建项目结构 (ID: 1)`）。
    *   这使得 AI 在多轮对话中始终“记得”当前任务进度。
*   **Capabilities & Tools (工具能力)**：列出可用工具（`execute_command`, `read_file`, `write_file` 等）。
*   **Constraints (约束)**：强制 JSON 输出格式、路径脱敏规则等。

### 3.2. 会话上下文 (Session Context)

*   **隔离机制**：每个会话（Session）拥有独立的 `session_id`。
*   **历史记录**：后端 `ChatRecordRepository` 自动检索该会话最近的 10 条交互记录（包含 User Input 和 AI Output），作为短期记忆注入到 LLM 请求中。
*   **CWD 持久化**：每个会话记录了最后一次所在的 CWD，确保用户切换会话回来后，依然停留在之前的目录。

### 3.3. 任务上下文 (Task Context)

这是实现"长程任务"的核心：

*   **任务链对象**：包含一组有序的任务节点，每个节点有 `id`, `desc` (描述), `status` (pending/in_progress/completed/failed)。
*   **动态更新**：
    *   前端维护 `currentTasks` 状态。
    *   后端在 Agent Loop 中实时解析 `task_update` 指令。
    *   当任务状态变更时，后端会自动更新 System Prompt 中的 "Current Task Context" 部分，强制 AI 基于最新进度进行下一步决策。

### 3.4. 文件系统上下文 (File System Context)

*   **沙箱隔离**：每个用户拥有独立的物理存储路径（`storageProperties.getAiTerminalAbsolute() + userId`）。
*   **路径映射**：
    *   **物理路径**：`D:\Users\...\ai_terminal_storage\1\project` (后端使用，严禁泄露)。
    *   **虚拟路径**：`/project` (前端和 AI 交互使用)。
    *   `TerminalServiceImpl` 负责处理物理路径与虚拟路径的双向转换与安全校验。

---

## 4. 执行机制 (Execution Mechanism)

### 4.1. 后端 Agent 循环 (Backend Agent Loop)

为了解决“AI 标记完任务就停止”的问题，后端实现了自动化的 Agent Loop (`AiChatServiceImpl.java`)：

```java
// 伪代码逻辑
while (loopCount < maxLoops) {
    // 1. 调用 LLM 获取回复
    String response = performBlockingChat(...);
    
    // 2. 解析回复中的 JSON
    if (response.contains("task_update")) {
        // 3. 提取任务状态变更
        updateTaskStatus(taskId, newStatus);
        
        // 4. 构建延续 Prompt
        // "任务 X 状态已更新为 Y。请继续执行该任务的具体操作..."
        currentPrompt = buildContinuationPrompt();
        
        // 5. 更新 System Prompt 上下文
        currentSystemPrompt = updateSystemPromptWithTasks(...);
        
        // 6. 继续循环 -> AI 收到新 Prompt 立即开始下一步
        continue;
    }
    
    // 如果没有任务更新或显式停止，退出循环
    break;
}
```

**机制优势**：
*   **原子性任务推进**：AI 可以在一次用户请求中，连续完成“思考 -> 标记任务完成 -> 开始下一步 -> 思考”的闭环，而无需用户反复输入“继续”。
*   **状态驱动**：循环的驱动力来自于任务状态的变更。

### 4.2. 前端命令执行 (Client-Side Command Execution)

实际的 Shell 命令执行由前端驱动，这是一个安全设计选择：

1.  **解析流**：前端 `TerminalView.vue` 实时监听 SSE 流。
2.  **指令识别**：当检测到 JSON 中包含 `tool: "execute_command"` 时。
3.  **API 调用**：前端调用后端 `/api/terminal/execute` 接口，发送 `command` 和 `cwd`。
4.  **后端执行**：
    *   `TerminalController` 接收请求。
    *   `TerminalService` 使用 `ProcessBuilder` (Windows/PowerShell) 执行命令。
    *   捕获 stdout/stderr，并返回新的 `cwd`（如果执行了 `cd` 命令）。
5.  **闭环反馈**：
    *   前端收到执行结果。
    *   **关键步骤**：前端将执行结果（Output）自动封装为一条新的消息，发送回后端（或直接触发下一轮 Agent 思考），告知 AI 命令的执行情况。

### 4.3. 流式输出与渲染优化

*   **SSE (Server-Sent Events)**：采用单向流技术，支持 `content` (正文) 和 `reasoning_content` (深度思考) 的并行传输。
*   **requestAnimationFrame**：前端使用 `requestAnimationFrame` 对高频的 SSE 数据包进行节流处理，确保在 1000+ 消息量级下界面渲染依然流畅，不会卡死浏览器主线程。

## 5. 总结

该机制通过**后端的状态循环**与**前端的命令执行能力**相结合，构建了一个既安全（沙箱隔离、路径脱敏）又智能（自动推理、任务链驱动）的 AI 终端环境。上下文的精细化管理确保了 AI 在多轮对话中始终保持清醒的目标感，而 Agent Loop 则赋予了 AI 连续解决复杂问题的行动力。
