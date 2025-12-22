# AI 终端重构计划：企业级 Agent 架构 (v4 Final Strict)

本计划是最终实施方案，严格遵循所有工程约束，确保系统具备生产级的健壮性、安全性和可维护性。

## 1. 核心协议定义 (Protocol)

### 1.1 Decision Envelope (Backend -> Frontend)
```json
{
  "decision_id": "uuid-v4",
  "type": "TOOL_CALL", // or TASK_COMPLETE, PAUSE, ERROR
  "action": "ensure_file",
  "params": { ... },
  "scope": {
    "allowed_paths": ["src/components/**"],
    "allowed_tasks": ["task-1"]
  },
  "expectation": {
    "world_change": ["src/components/Login.vue"],
    "task_progress": "Implement login UI"
  },
  "timeout_ms": 30000
}
```

### 1.2 ToolResult (Frontend -> Backend)
```json
{
  "decision_id": "uuid-v4", // 必须匹配
  "exit_code": 0,
  "stdout": "File created...",
  "stderr": "",
  "artifacts": ["src/components/Login.vue"]
}
```

### 1.3 StateMutator Result (Internal)
```json
{
  "accepted": true, // or false
  "reason": "Expectation met",
  "new_agent_status": "RUNNING" // or ERROR/BLOCKED
}
```

---

## 2. 后端架构实施 (Spring Boot)

### 2.1 实体层 (`com.aispring.entity.agent`)
*   **Enums**: `AgentStatus` (IDLE, PLANNING, RUNNING, WAITING_TOOL, PAUSED, COMPLETED, FROZEN, ERROR, BLOCKED).
*   **WorldState**: 增加 `FileMeta.source` (AGENT | USER | SYSTEM)。
*   **FSM**: 实现严格的状态迁移表 (e.g., `COMPLETED` -x-> `RUNNING`)。

### 2.2 服务层 (`com.aispring.service`)
*   **`AgentStateService`**:
    *   **Timeout Monitor**: 监控 `WAITING_TOOL` 状态，超时未收到 Result 则转 `ERROR`。
    *   **Source Tracking**: 记录文件变更来源。
*   **`StateMutator`**:
    *   **Validation**: 对比 `ToolResult` 与 `Expectation`。
    *   **Verdict**: 返回 `MutatorResult`，决定下一步是继续 RUNNING 还是 BLOCKED。
*   **`TaskCompiler`**:
    *   解析 Planner 输出，生成无自然语言残留的结构化 Task。

### 2.3 接口层 (`TerminalController`)
*   **Input Guard**: `RUNNING`/`WAITING_TOOL` 状态下，拒绝除 `pause/stop` 外的任何输入。
*   **Result Endpoint**: 接收 `ToolResult`，触发 `StateMutator`。

---

## 3. 前端架构实施 (Vue.js)

### 3.1 核心循环 (`processAgentLoop`)
*   **Decision Filter**: 检查 `action` 是否在白名单中。
*   **De-duplication**: 严格基于 `decision_id` 去重。
*   **No Auto-Retry**: 失败直接上报，等待后端新的 Decision。

### 3.2 UI 交互
*   **Input Lock**: 状态非 IDLE/PAUSED/ERROR 时，输入框锁定或仅接受指令。
*   **Status Visualization**: 清晰区分 `ERROR` (系统错) 与 `BLOCKED` (逻辑错)。
*   **Control Panel**: 提供 Resume (Retry), Stop (Abort) 按钮。

---

## 4. 实施与验证路线图

### Phase 1: Core Foundation (Backend)
1.  创建所有 Entity 和 DTO。
2.  实现 `AgentStateService` (Repository + FSM Logic)。
3.  实现 `TaskCompiler` 和 `StateMutator`。

### Phase 2: API & Protocol (Backend)
1.  重构 `TerminalController`，实现 Input Guard 和 Result 处理。
2.  更新 `PromptBuilder` 以生成 Decision Envelope 格式。

### Phase 3: Client Adaptation (Frontend)
1.  重构 Store，增加 `decision_id` 历史和 `AgentStatus`。
2.  重构 `processAgentLoop` 适配 ToolResult 协议。
3.  实现 Input Lock 和 Status UI。

### Phase 4: Verification (Tests)
1.  **Duplicate Decision**: 模拟发送两次相同 ID，验证前端仅执行一次。
2.  **Expectation Mismatch**: 模拟工具成功但文件未生成，验证后端转 BLOCKED。
3.  **Input Lock**: 运行时尝试发送聊天，验证被拒绝。
4.  **Recovery**: 验证从 PAUSED/ERROR 状态恢复流程。

