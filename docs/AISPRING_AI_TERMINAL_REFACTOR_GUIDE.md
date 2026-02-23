# AISpring AI 终端系统重构指南

## 文档版本信息
- **创建日期**：2025-12-23
- **目标项目**：AISpring (Spring Boot + Vue 3)
- **重构目标**：借鉴 Void-Main 设计，全面升级 AI 终端系统
- **参考文档**：[Void-Main AI 机制解析](./VOID_MAIN_AI_MECHANISM_ANALYSIS.md)

---

## 目录
1. [重构背景与目标](#1-重构背景与目标)
2. [当前系统分析](#2-当前系统分析)
3. [核心改进方案](#3-核心改进方案)
4. [数据库设计](#4-数据库设计)
5. [后端架构重构](#5-后端架构重构)
6. [前端架构重构](#6-前端架构重构)
7. [工具系统增强](#7-工具系统增强)
8. [检查点系统实现](#8-检查点系统实现)
9. [批准机制设计](#9-批准机制设计)
10. [中断机制优化](#10-中断机制优化)
11. [实施路线图](#11-实施路线图)
12. [测试策略](#12-测试策略)
13. [风险评估与缓解](#13-风险评估与缓解)

---

## 1. 重构背景与目标

### 1.1 当前痛点

| 问题 | 现状 | 影响 |
|------|------|------|
| **状态管理混乱** | `AgentState` 与 `ChatRecord` 分离存储 | 状态同步困难，易出现不一致 |
| **无检查点系统** | 无法回退到历史状态 | 用户无法撤销 AI 的错误操作 |
| **工具无批准机制** | 前端直接执行所有工具 | 存在安全风险（如删除文件） |
| **中断不完整** | LLM 可中断，但工具执行无法中断 | 用户体验差（无法停止长时间运行的命令） |
| **循环逻辑简陋** | 仅支持任务流水线，无通用循环 | 无法处理复杂的多步骤任务 |
| **工具结果格式化不统一** | 直接返回原始结果 | AI 难以解析结果，易产生幻觉 |
| **参数验证薄弱** | 前端直接传递参数 | 易出现运行时错误 |

### 1.2 重构目标

#### **目标 1：完整的会话状态管理**
- 将所有状态（消息、任务、文件快照）统一到 `ChatSession` 中
- 支持会话的完整导出/导入
- 实现增量状态更新（避免全量刷新）

#### **目标 2：检查点与时间旅行**
- 自动在关键时刻（用户消息前、工具编辑前）创建检查点
- 支持跳转到任意历史检查点
- 支持用户修改追踪（区分 AI 修改和用户修改）

#### **目标 3：工具批准机制**
- 支持工具分类批准（危险工具、文件编辑、MCP 工具等）
- 用户可配置自动批准规则
- 提供批准/拒绝的 UI 交互

#### **目标 4：统一中断机制**
- 为每个 Agent 循环生成唯一 `loopId`
- 支持中断 LLM 生成、工具执行、任务流水线
- 提供中断后的状态清理

#### **目标 5：工具系统增强**
- 严格的参数验证（类型、范围、格式）
- 统一的结果格式化（支持分页、错误提示）
- 工具执行结果的可中断性

#### **目标 6：Agent 循环优化**
- 支持通用的 Agent 循环（不依赖任务流水线）
- 自动重试机制（LLM 调用失败时）
- 流式状态的细粒度管理

---

## 2. 当前系统分析

### 2.1 现有架构图

```
┌───────────────────────────────────────────────────────────┐
│                     Vue 3 Frontend                         │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  TerminalView.vue                                   │  │
│  │  - SSE 流式监听                                      │  │
│  │  - 工具调用解析 (JSON.parse)                        │  │
│  │  - 工具执行 (executeCommand API)                    │  │
│  │  - 结果回传 (tool_result)                           │  │
│  └────────────────────┬────────────────────────────────┘  │
└────────────────────────┼────────────────────────────────────┘
                         │ HTTP/SSE
┌────────────────────────▼────────────────────────────────────┐
│               Spring Boot Backend                          │
│  ┌──────────────────────────────────────────────────────┐ │
│  │  TerminalController                                  │ │
│  │  - /chat-stream (SSE)                                │ │
│  │  - /execute (命令执行)                               │ │
│  │  - /write-file, /read-file (文件操作)               │ │
│  ├──────────────────────────────────────────────────────┤ │
│  │  AiChatServiceImpl                                   │ │
│  │  - askAgentStreamInternal (Agent 循环)              │ │
│  │  - performBlockingChat (LLM 调用)                   │ │
│  │  - 任务状态更新 (task_update)                       │ │
│  ├──────────────────────────────────────────────────────┤ │
│  │  AgentStateService                                   │ │
│  │  - 内存存储 (ConcurrentHashMap)                     │ │
│  │  - AgentState (status, taskState, lastDecision)     │ │
│  ├──────────────────────────────────────────────────────┤ │
│  │  TerminalServiceImpl                                 │ │
│  │  - executeCommand (ProcessBuilder)                   │ │
│  │  - writeFile, readFile, listFiles                    │ │
│  │  - 路径脱敏 (sanitizeOutput)                        │ │
│  └──────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 核心类分析

#### **2.2.1 AgentState**
```java
public class AgentState {
    private AgentStatus status;           // IDLE, PLANNING, RUNNING, WAITING_TOOL, PAUSED, COMPLETED
    private TaskState taskState;          // 任务流水线（可选）
    private DecisionEnvelope lastDecision; // 最后一次决策
}
```

**问题**：
- 状态存储在内存中，服务重启后丢失
- 与 `ChatRecord` 分离，需要手动同步
- 缺少流式状态（displayContentSoFar, reasoningSoFar）

#### **2.2.2 TerminalController**
```java
@PostMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter chatStream(@RequestBody TerminalChatRequest request) {
    // 1. 加载 AgentState
    AgentState state = agentStateService.getAgentState(sessionId, userId);
    
    // 2. 处理 tool_result（工具结果反馈）
    if (request.getTool_result() != null) {
        MutatorResult result = stateMutator.applyToolResult(state, request.getTool_result());
        if (!result.isAccepted()) {
            return sendSystemMessage("工具结果被拒绝：" + result.getReason());
        }
        
        // 自动继续执行
        String continuationPrompt = buildContinuationPrompt(state, request.getTool_result());
        return aiChatService.askAgentStream(continuationPrompt, sessionId, model, userId, systemPrompt, null, ...);
    }
    
    // 3. 意图分类 (PLAN / EXECUTE / CHAT)
    String intent = aiChatService.ask(promptManager.getIntentClassifierPrompt(request.getPrompt()), ...);
    if (intent.contains("PLAN")) {
        state.setStatus(AgentStatus.PLANNING);
        systemPrompt = promptManager.getPlannerPrompt(request.getPrompt());
    } else if (intent.contains("EXECUTE")) {
        state.setStatus(AgentStatus.RUNNING);
        systemPrompt = promptManager.getExecutorPrompt(agentPromptBuilder.buildPromptContext(state));
    }
    
    // 4. 流式响应
    return aiChatService.askAgentStream(request.getPrompt(), sessionId, model, userId, systemPrompt, null, 
        (fullResponse) -> handleAgentResponse(fullResponse, sessionId, userId));
}
```

**问题**：
- 意图分类需要额外的 LLM 调用（增加延迟）
- `handleAgentResponse` 仅在 Agent 循环外调用一次，无法处理中间状态
- 缺少统一的中断机制

#### **2.2.3 AiChatServiceImpl (Agent 循环)**
```java
private SseEmitter askAgentStreamInternal(String initialPrompt, ...) {
    new Thread(() -> {
        int loopCount = 0;
        int maxLoops = 10;
        
        while (loopCount < maxLoops) {
            loopCount++;
            String fullResponse = performBlockingChat(currentPrompt, sessionId, model, userId, currentSystemPrompt, emitter, fullContent);
            
            if (onResponse != null) {
                onResponse.accept(fullResponse); // 调用 handleAgentResponse
            }
            
            // 解析任务更新
            try {
                String json = extractJson(fullResponse);
                JsonNode root = objectMapper.readTree(json);
                if (root.has("type") && "task_update".equals(root.get("type").asText())) {
                    // 更新任务状态
                    String taskId = root.path("taskId").asText();
                    String status = root.path("status").asText();
                    updateTaskInList(currentTasks, taskId, status);
                    
                    // 更新 System Prompt
                    currentSystemPrompt = updateSystemPromptWithTasks(currentSystemPrompt, currentTasks);
                    
                    // 构建继续执行的 Prompt
                    currentPrompt = String.format("任务 %s (ID: %s) 状态已更新为 %s。请继续执行该任务的具体操作，或进行下一步。", desc, taskId, status);
                    continue; // 继续循环
                }
            } catch (Exception e) {
                log.error("Error parsing agent response for loop: {}", e.getMessage());
            }
            
            break; // 没有任务更新，退出循环
        }
        
        emitter.send(SseEmitter.event().data("[DONE]"));
        emitter.complete();
    }).start();
    
    return emitter;
}
```

**问题**：
- 循环驱动力仅依赖 `task_update`，无法处理通用的工具调用循环
- 缺少重试机制（LLM 调用失败时直接退出）
- 缺少细粒度的状态管理（无法区分 LLM 流式、工具执行、等待批准）

### 2.3 前端工具执行流程

```javascript
// TerminalView.vue
watch(() => terminal.value, (newContent) => {
  if (!newContent) return;
  
  try {
    const lines = newContent.split('\n');
    for (const line of lines) {
      if (line.trim().startsWith('{') && line.includes('"tool"')) {
        const decision = JSON.parse(line);
        
        if (decision.tool === 'execute_command') {
          // 立即执行命令
          executeCommand(decision.args.command, decision.args.cwd);
        }
      }
    }
  } catch (e) {
    console.error('Parse error:', e);
  }
});

async function executeCommand(command, cwd) {
  const response = await fetch('/api/terminal/execute', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ command, cwd, sessionId: currentSessionId.value })
  });
  
  const result = await response.json();
  
  // 回传结果
  await fetch('/api/terminal/chat-stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      session_id: currentSessionId.value,
      tool_result: {
        exitCode: result.data.exitCode,
        stdout: result.data.stdout,
        stderr: result.data.stderr,
        cwd: result.data.cwd
      }
    })
  });
}
```

**问题**：
- 工具执行没有批准机制，存在安全风险
- JSON 解析逻辑脆弱（依赖 LLM 输出格式）
- 缺少中断支持（无法停止正在执行的命令）

---

## 3. 核心改进方案

### 3.1 整体架构优化

```
┌──────────────────────────────────────────────────────────────┐
│                     Vue 3 Frontend                            │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  AgentTerminalView.vue (重构后)                        │  │
│  │  - 订阅 SSE 事件                                        │  │
│  │  - 状态机管理 (streaming_llm, running_tool, ...)      │  │
│  │  - 批准 UI (ApprovalDialog.vue)                        │  │
│  │  - 中断按钮 (stopButton)                               │  │
│  │  - 检查点导航 (CheckpointTimeline.vue)                 │  │
│  └────────────────────┬───────────────────────────────────┘  │
└────────────────────────┼───────────────────────────────────────┘
                         │ HTTP/SSE/WebSocket
┌────────────────────────▼───────────────────────────────────────┐
│               Spring Boot Backend (重构后)                     │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │  AgentController (新增)                                  │ │
│  │  - /agent/chat-stream (SSE)                              │ │
│  │  - /agent/approve-tool (批准工具)                        │ │
│  │  - /agent/reject-tool (拒绝工具)                         │ │
│  │  - /agent/interrupt (中断循环)                           │ │
│  │  - /agent/jump-to-checkpoint (时间旅行)                  │ │
│  ├──────────────────────────────────────────────────────────┤ │
│  │  AgentLoopService (新增 - 核心 Agent 循环)             │ │
│  │  - runAgentLoop(sessionId, userMessage)                 │ │
│  │  - runToolCall(sessionId, toolName, params)             │ │
│  │  - interruptLoop(loopId)                                │ │
│  │  - retryLLMCall(sessionId)                              │ │
│  ├──────────────────────────────────────────────────────────┤ │
│  │  SessionStateService (新增 - 替代 AgentStateService)   │ │
│  │  - Redis 存储 (分布式)                                   │ │
│  │  - SessionState (包含所有运行时状态)                    │ │
│  │  - StreamState (LLM 流式状态)                           │ │
│  ├──────────────────────────────────────────────────────────┤ │
│  │  CheckpointService (新增)                               │ │
│  │  - createCheckpoint(sessionId, type)                    │ │
│  │  - jumpToCheckpoint(sessionId, checkpointId)            │ │
│  │  - getCheckpointsBetween(sessionId, fromIdx, toIdx)     │ │
│  ├──────────────────────────────────────────────────────────┤ │
│  │  ToolValidationService (新增)                           │ │
│  │  - validateParams(toolName, rawParams)                  │ │
│  │  - formatResult(toolName, result)                       │ │
│  ├──────────────────────────────────────────────────────────┤ │
│  │  ToolApprovalService (新增)                             │ │
│  │  - checkIfNeedApproval(toolName, params)                │ │
│  │  - getUserApprovalSettings(userId)                      │ │
│  └──────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

### 3.2 状态机设计

#### **SessionState（会话状态）**
```java
@Data
@Builder
public class SessionState {
    private String sessionId;
    private Long userId;
    private AgentStatus status;           // IDLE, RUNNING, AWAITING_APPROVAL, PAUSED, COMPLETED, ERROR
    private String currentLoopId;         // 当前 Agent 循环 ID（用于中断）
    private StreamState streamState;      // 流式状态
    private TaskState taskState;          // 任务流水线（可选）
    private DecisionEnvelope lastDecision; // 最后一次决策
    private String lastCheckpointId;      // 最后一个检查点 ID
    private Instant createdAt;
    private Instant updatedAt;
}
```

#### **StreamState（流式状态）**
```java
@Data
@Builder
public class StreamState {
    private StreamType type;              // STREAMING_LLM, RUNNING_TOOL, AWAITING_USER, IDLE
    
    // LLM 流式状态
    private String displayContentSoFar;
    private String reasoningSoFar;
    private ToolCallDto toolCallSoFar;
    
    // 工具执行状态
    private String toolName;
    private Map<String, Object> toolParams;
    private String toolId;
    private String toolContent;
    
    // 中断支持
    private boolean interruptRequested;
}

public enum StreamType {
    STREAMING_LLM,    // 正在流式生成
    RUNNING_TOOL,     // 正在执行工具
    AWAITING_USER,    // 等待用户批准
    IDLE              // 空闲
}
```

#### **AgentStatus（Agent 状态）**
```java
public enum AgentStatus {
    IDLE,              // 空闲（等待用户输入）
    RUNNING,           // 正在运行 Agent 循环
    AWAITING_APPROVAL, // 等待用户批准工具
    PAUSED,            // 用户暂停
    COMPLETED,         // 任务完成
    ERROR              // 发生错误
}
```

**状态转换图**：
```
IDLE
  │
  ├──[用户发送消息]──→ RUNNING
  │
RUNNING
  │
  ├──[LLM 返回工具调用]──→ [需要批准?]
  │                          │
  │                          ├── Yes → AWAITING_APPROVAL
  │                          │           │
  │                          │           ├──[用户批准]──→ RUNNING
  │                          │           ├──[用户拒绝]──→ IDLE
  │                          │           └──[用户中断]──→ IDLE
  │                          │
  │                          └── No → RUNNING (自动执行工具并继续循环)
  │
  ├──[LLM 没有工具调用]──→ IDLE
  │
  ├──[用户点击中断]──→ IDLE
  │
  └──[发生错误]──→ ERROR
```

---

## 4. 数据库设计

### 4.1 表结构设计

#### **4.1.1 chat_checkpoints（检查点表）**
```sql
CREATE TABLE chat_checkpoints (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    checkpoint_id VARCHAR(64) NOT NULL UNIQUE COMMENT '检查点唯一标识（UUID）',
    session_id VARCHAR(100) NOT NULL COMMENT '会话 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    checkpoint_type ENUM('USER_MESSAGE', 'TOOL_EDIT', 'MANUAL') NOT NULL COMMENT '检查点类型',
    message_order INT NOT NULL COMMENT '消息顺序（关联到 chat_records）',
    
    -- 文件快照（JSON 格式）
    file_snapshots JSON COMMENT '文件快照映射 {fsPath: {fileContent, diffAreas}}',
    user_modifications JSON COMMENT '用户修改快照（区分 AI 修改和用户修改）',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_session_user (session_id, user_id),
    INDEX idx_message_order (session_id, message_order),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 终端检查点表';
```

**file_snapshots JSON 结构示例**：
```json
{
  "/project/src/Main.java": {
    "fileContent": "public class Main {\n    public static void main(String[] args) {\n        System.out.println(\"Hello World\");\n    }\n}",
    "diffAreas": [
      {
        "startLine": 3,
        "endLine": 3,
        "type": "modified",
        "originalContent": "        System.out.println(\"Hello\");",
        "newContent": "        System.out.println(\"Hello World\");"
      }
    ]
  }
}
```

#### **4.1.2 tool_approvals（工具批准记录表）**
```sql
CREATE TABLE tool_approvals (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(100) NOT NULL COMMENT '会话 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    tool_name VARCHAR(100) NOT NULL COMMENT '工具名称',
    tool_params JSON NOT NULL COMMENT '工具参数',
    approval_status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING' COMMENT '批准状态',
    decision_id VARCHAR(64) NOT NULL UNIQUE COMMENT '决策 ID（关联 DecisionEnvelope）',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP NULL COMMENT '批准时间',
    
    INDEX idx_session_user (session_id, user_id),
    INDEX idx_decision (decision_id),
    INDEX idx_status (approval_status),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具批准记录表';
```

#### **4.1.3 user_approval_settings（用户批准设置表）**
```sql
CREATE TABLE user_approval_settings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户 ID',
    
    -- 自动批准开关
    auto_approve_dangerous_tools BOOLEAN NOT NULL DEFAULT FALSE COMMENT '自动批准危险工具（删除、执行命令）',
    auto_approve_read_file BOOLEAN NOT NULL DEFAULT TRUE COMMENT '自动批准读取文件',
    auto_approve_file_edits BOOLEAN NOT NULL DEFAULT FALSE COMMENT '自动批准文件编辑',
    auto_approve_mcp_tools BOOLEAN NOT NULL DEFAULT FALSE COMMENT '自动批准 MCP 工具',
    
    -- 其他设置
    include_tool_lint_errors BOOLEAN NOT NULL DEFAULT TRUE COMMENT '工具执行后显示 Lint 错误',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户批准设置表';
```

#### **4.1.4 chat_records 表扩展**
```sql
ALTER TABLE chat_records
ADD COLUMN checkpoint_id VARCHAR(64) NULL COMMENT '关联的检查点 ID（如果此消息后有检查点）',
ADD COLUMN loop_id VARCHAR(64) NULL COMMENT 'Agent 循环 ID（用于中断）',
ADD COLUMN tool_approval_id BIGINT NULL COMMENT '关联的工具批准记录 ID',
ADD INDEX idx_checkpoint (checkpoint_id),
ADD INDEX idx_loop (loop_id),
ADD FOREIGN KEY (tool_approval_id) REFERENCES tool_approvals(id) ON DELETE SET NULL;
```

### 4.2 Redis 存储设计

**SessionState 存储**（键：`session:state:{sessionId}`）：
```json
{
  "sessionId": "sess-123",
  "userId": 21,
  "status": "RUNNING",
  "currentLoopId": "loop-456",
  "streamState": {
    "type": "STREAMING_LLM",
    "displayContentSoFar": "正在分析您的项目...",
    "reasoningSoFar": "首先我需要查看项目结构",
    "toolCallSoFar": null,
    "interruptRequested": false
  },
  "taskState": {
    "pipelineId": "pipe-789",
    "currentTaskId": "task-1",
    "tasks": [...]
  },
  "lastCheckpointId": "ckpt-abc",
  "createdAt": "2025-12-23T10:00:00Z",
  "updatedAt": "2025-12-23T10:05:00Z"
}
```

**TTL 策略**：
- 活跃会话：保留 24 小时
- 非活跃会话：保留 1 小时
- 用户离线：立即持久化到数据库并清除 Redis

---

## 5. 后端架构重构

### 5.1 核心服务层

#### **5.1.1 AgentLoopService（核心 Agent 循环服务）**

**接口定义**：
```java
public interface AgentLoopService {
    /**
     * 运行 Agent 循环
     * @return loopId（用于中断）
     */
    String runAgentLoop(String sessionId, Long userId, String userMessage);
    
    /**
     * 批准并执行工具
     */
    void approveAndRunTool(String sessionId, Long userId, String decisionId);
    
    /**
     * 拒绝工具
     */
    void rejectTool(String sessionId, Long userId, String decisionId);
    
    /**
     * 中断 Agent 循环
     */
    void interruptLoop(String loopId);
    
    /**
     * 获取循环状态
     */
    SessionState getLoopState(String sessionId);
}
```

**实现逻辑**（伪代码）：
```java
@Service
@Slf4j
public class AgentLoopServiceImpl implements AgentLoopService {
    
    private final SessionStateService sessionStateService;
    private final LLMService llmService;
    private final ToolValidationService toolValidationService;
    private final ToolApprovalService toolApprovalService;
    private final ToolExecutionService toolExecutionService;
    private final CheckpointService checkpointService;
    private final SseEmitterManager sseEmitterManager;
    
    // 存储所有活跃的循环
    private final ConcurrentHashMap<String, CompletableFuture<Void>> activeLoops = new ConcurrentHashMap<>();
    
    @Override
    public String runAgentLoop(String sessionId, Long userId, String userMessage) {
        // 1. 生成 loopId
        String loopId = UUID.randomUUID().toString();
        
        // 2. 加载会话状态
        SessionState state = sessionStateService.getSessionState(sessionId, userId);
        state.setCurrentLoopId(loopId);
        state.setStatus(AgentStatus.RUNNING);
        sessionStateService.saveSessionState(state);
        
        // 3. 添加用户消息到历史
        ChatRecord userRecord = chatRecordService.createChatRecord(
            userMessage, 1, userId.toString(), sessionId, null, "completed", "terminal"
        );
        
        // 4. 创建用户消息前的检查点
        if (chatRecordService.getSessionMessages(userId.toString(), sessionId).size() == 1) {
            checkpointService.createCheckpoint(sessionId, userId, CheckpointType.USER_MESSAGE, userRecord.getMessageOrder());
        }
        
        // 5. 异步运行 Agent 循环
        CompletableFuture<Void> loopFuture = CompletableFuture.runAsync(() -> {
            runAgentLoopInternal(loopId, sessionId, userId, userMessage);
        });
        
        activeLoops.put(loopId, loopFuture);
        
        // 6. 循环结束后清理
        loopFuture.whenComplete((result, error) -> {
            activeLoops.remove(loopId);
            if (error != null) {
                log.error("Agent loop error: {}", error.getMessage(), error);
                state.setStatus(AgentStatus.ERROR);
                sessionStateService.saveSessionState(state);
            }
        });
        
        return loopId;
    }
    
    private void runAgentLoopInternal(String loopId, String sessionId, Long userId, String initialMessage) {
        SessionState state = sessionStateService.getSessionState(sessionId, userId);
        SseEmitter emitter = sseEmitterManager.getEmitter(sessionId);
        
        int nMessagesSent = 0;
        int maxLoops = 10;
        boolean shouldContinue = true;
        
        while (shouldContinue && nMessagesSent < maxLoops) {
            // 检查是否被中断
            if (state.getStreamState().isInterruptRequested()) {
                log.info("Agent loop interrupted: {}", loopId);
                break;
            }
            
            nMessagesSent++;
            
            // 设置状态为 STREAMING_LLM
            state.setStreamState(StreamState.builder()
                .type(StreamType.STREAMING_LLM)
                .displayContentSoFar("")
                .reasoningSoFar("")
                .toolCallSoFar(null)
                .interruptRequested(false)
                .build());
            sessionStateService.saveSessionState(state);
            
            // 准备消息历史
            List<LLMMessage> messages = prepareMessages(sessionId, userId);
            
            // 重试机制
            int nAttempts = 0;
            int maxAttempts = 3;
            boolean shouldRetry = true;
            LLMResponse llmResponse = null;
            
            while (shouldRetry && nAttempts < maxAttempts) {
                nAttempts++;
                shouldRetry = false;
                
                try {
                    // 调用 LLM（流式）
                    llmResponse = llmService.sendMessageStream(
                        messages,
                        (chunk) -> {
                            // 流式回调：更新 StreamState 并发送 SSE
                            StreamState streamState = state.getStreamState();
                            streamState.setDisplayContentSoFar(streamState.getDisplayContentSoFar() + chunk.getContent());
                            streamState.setReasoningSoFar(streamState.getReasoningSoFar() + chunk.getReasoningContent());
                            if (chunk.getToolCall() != null) {
                                streamState.setToolCallSoFar(chunk.getToolCall());
                            }
                            sessionStateService.saveSessionState(state);
                            
                            // 发送 SSE
                            sseEmitterManager.send(sessionId, "stream_update", streamState);
                        }
                    );
                } catch (Exception e) {
                    log.error("LLM call failed (attempt {}): {}", nAttempts, e.getMessage());
                    if (nAttempts < maxAttempts) {
                        shouldRetry = true;
                        Thread.sleep(2000); // 等待 2 秒后重试
                        continue;
                    } else {
                        // 重试失败，添加错误消息
                        chatRecordService.createChatRecord(
                            "LLM 调用失败：" + e.getMessage(), 2, userId.toString(), sessionId, 
                            null, "error", "terminal"
                        );
                        state.setStatus(AgentStatus.ERROR);
                        sessionStateService.saveSessionState(state);
                        return;
                    }
                }
            }
            
            // 添加 AI 响应到历史
            chatRecordService.createChatRecord(
                llmResponse.getContent(), 2, userId.toString(), sessionId, 
                llmResponse.getModel(), "completed", "terminal", llmResponse.getReasoningContent()
            );
            
            // 处理工具调用
            if (llmResponse.getToolCall() != null) {
                ToolCallDto toolCall = llmResponse.getToolCall();
                
                // 参数验证
                Map<String, Object> validatedParams;
                try {
                    validatedParams = toolValidationService.validateParams(toolCall.getName(), toolCall.getRawParams());
                } catch (ValidationException e) {
                    // 参数验证失败，添加错误消息并退出循环
                    chatRecordService.createChatRecord(
                        "工具参数验证失败：" + e.getMessage(), 3, userId.toString(), sessionId, 
                        null, "error", "terminal"
                    );
                    shouldContinue = false;
                    continue;
                }
                
                // 创建编辑检查点（针对文件编辑工具）
                if ("edit_file".equals(toolCall.getName()) || "rewrite_file".equals(toolCall.getName())) {
                    checkpointService.createCheckpoint(sessionId, userId, CheckpointType.TOOL_EDIT, null);
                }
                
                // 检查是否需要批准
                boolean needsApproval = toolApprovalService.checkIfNeedApproval(toolCall.getName(), validatedParams, userId);
                
                if (needsApproval) {
                    // 保存批准请求
                    ToolApproval approval = toolApprovalService.createApprovalRequest(
                        sessionId, userId, toolCall.getName(), validatedParams, toolCall.getId()
                    );
                    
                    // 更新状态为 AWAITING_APPROVAL
                    state.setStatus(AgentStatus.AWAITING_APPROVAL);
                    state.setStreamState(StreamState.builder()
                        .type(StreamType.AWAITING_USER)
                        .toolName(toolCall.getName())
                        .toolParams(validatedParams)
                        .toolId(toolCall.getId())
                        .build());
                    sessionStateService.saveSessionState(state);
                    
                    // 发送 SSE 通知前端
                    sseEmitterManager.send(sessionId, "tool_approval_required", approval);
                    
                    // 退出循环，等待用户批准
                    shouldContinue = false;
                } else {
                    // 自动批准，立即执行工具
                    ToolResult result = executeToolAndContinue(loopId, sessionId, userId, toolCall, validatedParams);
                    
                    if (result.isSuccess()) {
                        shouldContinue = true; // 继续循环
                    } else {
                        shouldContinue = false; // 工具执行失败，退出循环
                    }
                }
            } else {
                // 没有工具调用，退出循环
                shouldContinue = false;
            }
        }
        
        // 循环结束，创建用户消息前的检查点
        if (state.getStatus() != AgentStatus.AWAITING_APPROVAL) {
            checkpointService.createCheckpoint(sessionId, userId, CheckpointType.USER_MESSAGE, null);
            state.setStatus(AgentStatus.IDLE);
            sessionStateService.saveSessionState(state);
        }
        
        // 发送循环完成事件
        sseEmitterManager.send(sessionId, "agent_loop_done", Map.of("loopId", loopId, "nMessagesSent", nMessagesSent));
    }
    
    private ToolResult executeToolAndContinue(String loopId, String sessionId, Long userId, ToolCallDto toolCall, Map<String, Object> validatedParams) {
        SessionState state = sessionStateService.getSessionState(sessionId, userId);
        
        // 更新状态为 RUNNING_TOOL
        state.setStreamState(StreamState.builder()
            .type(StreamType.RUNNING_TOOL)
            .toolName(toolCall.getName())
            .toolParams(validatedParams)
            .toolId(toolCall.getId())
            .toolContent("(正在执行...)")
            .build());
        sessionStateService.saveSessionState(state);
        
        // 执行工具
        ToolResult result;
        try {
            result = toolExecutionService.executeTool(toolCall.getName(), validatedParams, (progress) -> {
                // 工具执行进度回调
                state.getStreamState().setToolContent(progress);
                sessionStateService.saveSessionState(state);
                sseEmitterManager.send(sessionId, "tool_progress", Map.of("content", progress));
            });
        } catch (Exception e) {
            log.error("Tool execution error: {}", e.getMessage(), e);
            result = ToolResult.error(e.getMessage());
        }
        
        // 格式化结果
        String formattedResult = toolValidationService.formatResult(toolCall.getName(), result);
        
        // 添加工具结果到历史
        chatRecordService.createChatRecord(
            formattedResult, 3, userId.toString(), sessionId, 
            null, result.isSuccess() ? "completed" : "error", "terminal"
        );
        
        return result;
    }
    
    @Override
    public void approveAndRunTool(String sessionId, Long userId, String decisionId) {
        // 1. 获取批准请求
        ToolApproval approval = toolApprovalService.getApprovalByDecisionId(decisionId);
        if (approval == null || !approval.getApprovalStatus().equals(ApprovalStatus.PENDING)) {
            throw new IllegalStateException("Invalid approval request");
        }
        
        // 2. 更新批准状态
        approval.setApprovalStatus(ApprovalStatus.APPROVED);
        approval.setApprovedAt(Instant.now());
        toolApprovalService.saveApproval(approval);
        
        // 3. 执行工具
        SessionState state = sessionStateService.getSessionState(sessionId, userId);
        String loopId = state.getCurrentLoopId();
        
        ToolCallDto toolCall = ToolCallDto.builder()
            .name(approval.getToolName())
            .id(approval.getDecisionId())
            .rawParams(approval.getToolParams())
            .build();
        
        ToolResult result = executeToolAndContinue(loopId, sessionId, userId, toolCall, approval.getToolParams());
        
        // 4. 继续 Agent 循环
        if (result.isSuccess()) {
            state.setStatus(AgentStatus.RUNNING);
            sessionStateService.saveSessionState(state);
            
            // 重新进入循环
            runAgentLoop(sessionId, userId, "");
        } else {
            state.setStatus(AgentStatus.IDLE);
            sessionStateService.saveSessionState(state);
        }
    }
    
    @Override
    public void rejectTool(String sessionId, Long userId, String decisionId) {
        // 1. 获取批准请求
        ToolApproval approval = toolApprovalService.getApprovalByDecisionId(decisionId);
        if (approval == null || !approval.getApprovalStatus().equals(ApprovalStatus.PENDING)) {
            throw new IllegalStateException("Invalid approval request");
        }
        
        // 2. 更新批准状态
        approval.setApprovalStatus(ApprovalStatus.REJECTED);
        approval.setApprovedAt(Instant.now());
        toolApprovalService.saveApproval(approval);
        
        // 3. 添加拒绝消息
        chatRecordService.createChatRecord(
            "工具调用被用户拒绝：" + approval.getToolName(), 3, userId.toString(), sessionId, 
            null, "rejected", "terminal"
        );
        
        // 4. 更新状态为 IDLE
        SessionState state = sessionStateService.getSessionState(sessionId, userId);
        state.setStatus(AgentStatus.IDLE);
        state.setStreamState(StreamState.builder().type(StreamType.IDLE).build());
        sessionStateService.saveSessionState(state);
        
        // 5. 创建检查点
        checkpointService.createCheckpoint(sessionId, userId, CheckpointType.USER_MESSAGE, null);
    }
    
    @Override
    public void interruptLoop(String loopId) {
        // 1. 查找活跃的循环
        CompletableFuture<Void> loopFuture = activeLoops.get(loopId);
        if (loopFuture == null) {
            log.warn("Loop not found: {}", loopId);
            return;
        }
        
        // 2. 设置中断标志（通过 SessionState）
        // 注意：这里需要通过 loopId 反查 sessionId
        SessionState state = sessionStateService.getSessionStateByLoopId(loopId);
        if (state != null) {
            state.getStreamState().setInterruptRequested(true);
            sessionStateService.saveSessionState(state);
        }
        
        // 3. 取消 Future（如果支持）
        loopFuture.cancel(true);
        
        activeLoops.remove(loopId);
    }
}
```

#### **5.1.2 CheckpointService（检查点服务）**

**接口定义**：
```java
public interface CheckpointService {
    /**
     * 创建检查点
     */
    String createCheckpoint(String sessionId, Long userId, CheckpointType type, Integer messageOrder);
    
    /**
     * 跳转到检查点
     */
    void jumpToCheckpoint(String sessionId, Long userId, String checkpointId, boolean includeUserModifications);
    
    /**
     * 获取会话的所有检查点
     */
    List<Checkpoint> getCheckpoints(String sessionId, Long userId);
    
    /**
     * 获取两个检查点之间的文件变更
     */
    Map<String, FileSnapshot> getFileChangesBetween(String sessionId, Long userId, String fromCheckpointId, String toCheckpointId);
    
    /**
     * 添加用户修改到当前检查点
     */
    void addUserModifications(String sessionId, Long userId, Map<String, FileSnapshot> modifications);
}
```

**核心实现**（跳转到检查点）：
```java
@Service
@Slf4j
public class CheckpointServiceImpl implements CheckpointService {
    
    private final CheckpointRepository checkpointRepository;
    private final FileSystemService fileSystemService;
    private final DiffService diffService;
    
    @Override
    public void jumpToCheckpoint(String sessionId, Long userId, String checkpointId, boolean includeUserModifications) {
        // 1. 获取目标检查点
        Checkpoint targetCheckpoint = checkpointRepository.findByCheckpointId(checkpointId)
            .orElseThrow(() -> new IllegalArgumentException("Checkpoint not found: " + checkpointId));
        
        if (!targetCheckpoint.getSessionId().equals(sessionId) || !targetCheckpoint.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Checkpoint does not belong to this session");
        }
        
        // 2. 获取当前检查点
        Checkpoint currentCheckpoint = checkpointRepository.findTopBySessionIdAndUserIdOrderByMessageOrderDesc(sessionId, userId)
            .orElse(null);
        
        if (currentCheckpoint == null) {
            log.warn("No current checkpoint found, creating one");
            createCheckpoint(sessionId, userId, CheckpointType.MANUAL, null);
            currentCheckpoint = checkpointRepository.findTopBySessionIdAndUserIdOrderByMessageOrderDesc(sessionId, userId)
                .orElseThrow();
        }
        
        // 3. 保存当前用户修改到当前检查点
        Map<String, FileSnapshot> currentModifications = computeUserModifications(sessionId, userId, currentCheckpoint);
        currentCheckpoint.setUserModifications(currentModifications);
        checkpointRepository.save(currentCheckpoint);
        
        // 4. 确定跳转方向（撤销 vs 重做）
        int fromOrder = currentCheckpoint.getMessageOrder();
        int toOrder = targetCheckpoint.getMessageOrder();
        
        if (toOrder < fromOrder) {
            // 撤销：恢复 from 和 to 之间修改过的文件
            undoChanges(sessionId, userId, toOrder, fromOrder, targetCheckpoint, includeUserModifications);
        } else if (toOrder > fromOrder) {
            // 重做：应用 from 和 to 之间的最新修改
            redoChanges(sessionId, userId, fromOrder, toOrder, targetCheckpoint, includeUserModifications);
        }
        
        // 5. 更新会话的当前检查点
        SessionState state = sessionStateService.getSessionState(sessionId, userId);
        state.setLastCheckpointId(checkpointId);
        sessionStateService.saveSessionState(state);
    }
    
    private void undoChanges(String sessionId, Long userId, int toOrder, int fromOrder, Checkpoint targetCheckpoint, boolean includeUserModifications) {
        // 获取所有在 from 和 to 之间修改过的文件
        Map<String, Integer> lastCheckpointOfFile = new HashMap<>();
        List<Checkpoint> checkpoints = checkpointRepository.findBySessionIdAndUserIdAndMessageOrderBetween(
            sessionId, userId, toOrder + 1, fromOrder
        );
        
        for (Checkpoint checkpoint : checkpoints) {
            Map<String, FileSnapshot> fileSnapshots = checkpoint.getFileSnapshots();
            for (String fsPath : fileSnapshots.keySet()) {
                lastCheckpointOfFile.put(fsPath, checkpoint.getMessageOrder());
            }
        }
        
        // 对每个文件，查找最近的检查点并恢复
        for (Map.Entry<String, Integer> entry : lastCheckpointOfFile.entrySet()) {
            String fsPath = entry.getKey();
            
            // 向上查找（从 toOrder 开始）
            FileSnapshot snapshot = findSnapshotInRange(sessionId, userId, fsPath, toOrder, 0, includeUserModifications);
            
            if (snapshot == null) {
                // 向下查找（罕见情况）
                snapshot = findSnapshotInRange(sessionId, userId, fsPath, toOrder + 1, Integer.MAX_VALUE, includeUserModifications);
            }
            
            if (snapshot != null) {
                // 恢复文件
                fileSystemService.restoreFile(userId, fsPath, snapshot.getFileContent());
            }
        }
    }
    
    private void redoChanges(String sessionId, Long userId, int fromOrder, int toOrder, Checkpoint targetCheckpoint, boolean includeUserModifications) {
        // 获取所有在 from 和 to 之间修改过的文件
        Map<String, Integer> lastCheckpointOfFile = new HashMap<>();
        List<Checkpoint> checkpoints = checkpointRepository.findBySessionIdAndUserIdAndMessageOrderBetween(
            sessionId, userId, fromOrder + 1, toOrder
        );
        
        for (Checkpoint checkpoint : checkpoints) {
            Map<String, FileSnapshot> fileSnapshots = checkpoint.getFileSnapshots();
            for (String fsPath : fileSnapshots.keySet()) {
                lastCheckpointOfFile.put(fsPath, checkpoint.getMessageOrder());
            }
        }
        
        // 对每个文件，查找最新的检查点并应用
        for (Map.Entry<String, Integer> entry : lastCheckpointOfFile.entrySet()) {
            String fsPath = entry.getKey();
            
            // 向下查找（从 toOrder 开始）
            FileSnapshot snapshot = findSnapshotInRange(sessionId, userId, fsPath, toOrder, fromOrder + 1, includeUserModifications);
            
            if (snapshot != null) {
                // 应用文件变更
                fileSystemService.restoreFile(userId, fsPath, snapshot.getFileContent());
            }
        }
    }
    
    private FileSnapshot findSnapshotInRange(String sessionId, Long userId, String fsPath, int startOrder, int endOrder, boolean includeUserModifications) {
        List<Checkpoint> checkpoints;
        
        if (startOrder > endOrder) {
            // 向上查找
            checkpoints = checkpointRepository.findBySessionIdAndUserIdAndMessageOrderLessThanEqualOrderByMessageOrderDesc(
                sessionId, userId, startOrder
            );
        } else {
            // 向下查找
            checkpoints = checkpointRepository.findBySessionIdAndUserIdAndMessageOrderGreaterThanEqualOrderByMessageOrderAsc(
                sessionId, userId, startOrder
            );
        }
        
        for (Checkpoint checkpoint : checkpoints) {
            // 优先使用用户修改的快照
            if (includeUserModifications && checkpoint.getUserModifications() != null) {
                FileSnapshot snapshot = checkpoint.getUserModifications().get(fsPath);
                if (snapshot != null) {
                    return snapshot;
                }
            }
            
            // 使用 AI 修改的快照
            Map<String, FileSnapshot> fileSnapshots = checkpoint.getFileSnapshots();
            FileSnapshot snapshot = fileSnapshots.get(fsPath);
            if (snapshot != null) {
                return snapshot;
            }
        }
        
        return null;
    }
    
    private Map<String, FileSnapshot> computeUserModifications(String sessionId, Long userId, Checkpoint currentCheckpoint) {
        Map<String, FileSnapshot> modifications = new HashMap<>();
        
        // 获取当前检查点后所有修改过的文件
        Map<String, FileSnapshot> currentFiles = currentCheckpoint.getFileSnapshots();
        
        for (String fsPath : currentFiles.keySet()) {
            // 读取当前文件内容
            String currentContent = fileSystemService.readFile(userId, fsPath);
            
            // 对比检查点内容
            FileSnapshot checkpointSnapshot = currentFiles.get(fsPath);
            if (!currentContent.equals(checkpointSnapshot.getFileContent())) {
                // 文件有变更，记录用户修改
                modifications.put(fsPath, FileSnapshot.builder()
                    .fileContent(currentContent)
                    .diffAreas(diffService.computeDiff(checkpointSnapshot.getFileContent(), currentContent))
                    .build());
            }
        }
        
        return modifications;
    }
}
```

---

## 6. 前端架构重构

### 6.1 Vue 组件设计

#### **6.1.1 AgentTerminalView.vue（主视图）**

```vue
<template>
  <div class="agent-terminal">
    <!-- 头部工具栏 -->
    <div class="terminal-header">
      <div class="session-info">
        <span>会话: {{ currentSession?.title || '新对话' }}</span>
        <span class="status-badge" :class="agentStatusClass">{{ agentStatusText }}</span>
      </div>
      
      <div class="actions">
        <!-- 检查点时间线 -->
        <button @click="showCheckpointTimeline = true" :disabled="!hasCheckpoints">
          <i class="icon-history"></i> 历史
        </button>
        
        <!-- 中断按钮 -->
        <button 
          @click="interruptAgent" 
          :disabled="!isRunning"
          class="interrupt-btn"
        >
          <i class="icon-stop"></i> 中断
        </button>
        
        <!-- 设置 -->
        <button @click="showSettings = true">
          <i class="icon-settings"></i>
        </button>
      </div>
    </div>
    
    <!-- 消息列表 -->
    <div class="messages-container" ref="messagesContainer">
      <div 
        v-for="(message, index) in messages" 
        :key="message.id"
        :class="['message', `message-${message.senderType}`]"
      >
        <!-- 用户消息 -->
        <div v-if="message.senderType === 1" class="user-message">
          <div class="message-avatar">👤</div>
          <div class="message-content">{{ message.content }}</div>
        </div>
        
        <!-- AI 消息 -->
        <div v-else-if="message.senderType === 2" class="ai-message">
          <div class="message-avatar">🤖</div>
          <div class="message-content">
            <!-- 推理内容（可折叠） -->
            <div v-if="message.reasoningContent" class="reasoning-section">
              <button @click="toggleReasoning(message.id)">
                <i :class="isReasoningExpanded(message.id) ? 'icon-collapse' : 'icon-expand'"></i>
                思考过程
              </button>
              <div v-if="isReasoningExpanded(message.id)" class="reasoning-content">
                {{ message.reasoningContent }}
              </div>
            </div>
            
            <!-- 主要内容 -->
            <div class="main-content" v-html="renderMarkdown(message.content)"></div>
          </div>
        </div>
        
        <!-- 工具消息 -->
        <div v-else-if="message.senderType === 3" class="tool-message">
          <div class="tool-header">
            <i class="icon-tool"></i>
            <span>{{ message.toolName }}</span>
          </div>
          <div class="tool-content">
            <pre>{{ message.content }}</pre>
          </div>
        </div>
        
        <!-- 检查点标记 -->
        <div 
          v-if="message.checkpointId" 
          class="checkpoint-marker"
          @click="jumpToCheckpoint(message.checkpointId)"
        >
          <i class="icon-checkpoint"></i>
          <span>检查点</span>
        </div>
      </div>
      
      <!-- 流式内容（正在生成） -->
      <div v-if="isStreaming" class="streaming-message ai-message">
        <div class="message-avatar">🤖</div>
        <div class="message-content">
          <!-- 推理内容 -->
          <div v-if="streamState.reasoningSoFar" class="reasoning-section">
            <button>
              <i class="icon-thinking"></i> 正在思考...
            </button>
            <div class="reasoning-content">
              {{ streamState.reasoningSoFar }}
            </div>
          </div>
          
          <!-- 主要内容 -->
          <div class="main-content" v-html="renderMarkdown(streamState.displayContentSoFar)"></div>
          
          <!-- 打字机效果 -->
          <span class="cursor-blink">▋</span>
        </div>
      </div>
      
      <!-- 工具批准 UI -->
      <ToolApprovalDialog 
        v-if="pendingApproval"
        :tool-name="pendingApproval.toolName"
        :tool-params="pendingApproval.toolParams"
        @approve="approveToolCall"
        @reject="rejectToolCall"
      />
    </div>
    
    <!-- 输入框 -->
    <div class="input-container">
      <textarea
        v-model="userInput"
        @keydown.enter.ctrl="sendMessage"
        placeholder="输入您的指令... (Ctrl+Enter 发送)"
        :disabled="isRunning && !isAwaitingUser"
      ></textarea>
      <button 
        @click="sendMessage" 
        :disabled="!userInput.trim() || (isRunning && !isAwaitingUser)"
        class="send-btn"
      >
        <i class="icon-send"></i> 发送
      </button>
    </div>
    
    <!-- 检查点时间线弹窗 -->
    <CheckpointTimeline 
      v-if="showCheckpointTimeline"
      :session-id="currentSessionId"
      @close="showCheckpointTimeline = false"
      @jump="handleCheckpointJump"
    />
    
    <!-- 设置弹窗 -->
    <SettingsDialog
      v-if="showSettings"
      @close="showSettings = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import { marked } from 'marked';
import ToolApprovalDialog from './ToolApprovalDialog.vue';
import CheckpointTimeline from './CheckpointTimeline.vue';
import SettingsDialog from './SettingsDialog.vue';

const route = useRoute();
const currentSessionId = ref(route.params.sessionId || null);

// 状态
const messages = ref([]);
const userInput = ref('');
const streamState = ref({
  type: 'IDLE',
  displayContentSoFar: '',
  reasoningSoFar: '',
  toolCallSoFar: null
});
const agentStatus = ref('IDLE');
const pendingApproval = ref(null);
const expandedReasoning = ref(new Set());
const showCheckpointTimeline = ref(false);
const showSettings = ref(false);
const hasCheckpoints = ref(false);

// 计算属性
const isRunning = computed(() => {
  return ['RUNNING', 'AWAITING_APPROVAL'].includes(agentStatus.value);
});

const isAwaitingUser = computed(() => {
  return agentStatus.value === 'AWAITING_APPROVAL';
});

const isStreaming = computed(() => {
  return streamState.value.type === 'STREAMING_LLM' && streamState.value.displayContentSoFar;
});

const agentStatusClass = computed(() => {
  return `status-${agentStatus.value.toLowerCase()}`;
});

const agentStatusText = computed(() => {
  const statusMap = {
    'IDLE': '空闲',
    'RUNNING': '运行中',
    'AWAITING_APPROVAL': '等待批准',
    'PAUSED': '已暂停',
    'COMPLETED': '已完成',
    'ERROR': '错误'
  };
  return statusMap[agentStatus.value] || agentStatus.value;
});

// SSE 连接
let eventSource = null;

function connectSSE() {
  if (eventSource) {
    eventSource.close();
  }
  
  eventSource = new EventSource(`/api/agent/stream?sessionId=${currentSessionId.value}`);
  
  // 流式更新事件
  eventSource.addEventListener('stream_update', (event) => {
    const data = JSON.parse(event.data);
    streamState.value = data;
  });
  
  // 工具批准请求事件
  eventSource.addEventListener('tool_approval_required', (event) => {
    const approval = JSON.parse(event.data);
    pendingApproval.value = approval;
    agentStatus.value = 'AWAITING_APPROVAL';
  });
  
  // Agent 循环完成事件
  eventSource.addEventListener('agent_loop_done', (event) => {
    const data = JSON.parse(event.data);
    console.log('Agent loop done:', data);
    agentStatus.value = 'IDLE';
    streamState.value = { type: 'IDLE', displayContentSoFar: '', reasoningSoFar: '', toolCallSoFar: null };
    loadMessages(); // 重新加载消息
  });
  
  // 状态更新事件
  eventSource.addEventListener('status_update', (event) => {
    const data = JSON.parse(event.data);
    agentStatus.value = data.status;
  });
  
  // 错误处理
  eventSource.onerror = (error) => {
    console.error('SSE error:', error);
    eventSource.close();
    // 重连
    setTimeout(() => connectSSE(), 5000);
  };
}

// 发送消息
async function sendMessage() {
  if (!userInput.value.trim()) return;
  
  const message = userInput.value;
  userInput.value = '';
  
  try {
    const response = await fetch('/api/agent/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId: currentSessionId.value,
        message: message
      })
    });
    
    const data = await response.json();
    if (data.success) {
      agentStatus.value = 'RUNNING';
      // 添加用户消息到界面
      messages.value.push({
        id: Date.now(),
        senderType: 1,
        content: message,
        createdAt: new Date()
      });
      scrollToBottom();
    }
  } catch (error) {
    console.error('Send message error:', error);
  }
}

// 批准工具调用
async function approveToolCall() {
  if (!pendingApproval.value) return;
  
  try {
    await fetch('/api/agent/approve-tool', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId: currentSessionId.value,
        decisionId: pendingApproval.value.decisionId
      })
    });
    
    pendingApproval.value = null;
    agentStatus.value = 'RUNNING';
  } catch (error) {
    console.error('Approve tool error:', error);
  }
}

// 拒绝工具调用
async function rejectToolCall() {
  if (!pendingApproval.value) return;
  
  try {
    await fetch('/api/agent/reject-tool', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId: currentSessionId.value,
        decisionId: pendingApproval.value.decisionId
      })
    });
    
    pendingApproval.value = null;
    agentStatus.value = 'IDLE';
  } catch (error) {
    console.error('Reject tool error:', error);
  }
}

// 中断 Agent
async function interruptAgent() {
  try {
    await fetch('/api/agent/interrupt', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId: currentSessionId.value
      })
    });
    
    agentStatus.value = 'IDLE';
    streamState.value = { type: 'IDLE', displayContentSoFar: '', reasoningSoFar: '', toolCallSoFar: null };
  } catch (error) {
    console.error('Interrupt agent error:', error);
  }
}

// 跳转到检查点
async function jumpToCheckpoint(checkpointId) {
  try {
    await fetch('/api/agent/jump-to-checkpoint', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        sessionId: currentSessionId.value,
        checkpointId: checkpointId,
        includeUserModifications: true
      })
    });
    
    loadMessages();
  } catch (error) {
    console.error('Jump to checkpoint error:', error);
  }
}

// 加载消息
async function loadMessages() {
  try {
    const response = await fetch(`/api/terminal/history/${currentSessionId.value}`);
    const data = await response.json();
    if (data.success) {
      messages.value = data.data;
    }
  } catch (error) {
    console.error('Load messages error:', error);
  }
}

// 渲染 Markdown
function renderMarkdown(text) {
  if (!text) return '';
  return marked(text);
}

// 切换推理内容展开/折叠
function toggleReasoning(messageId) {
  if (expandedReasoning.value.has(messageId)) {
    expandedReasoning.value.delete(messageId);
  } else {
    expandedReasoning.value.add(messageId);
  }
}

function isReasoningExpanded(messageId) {
  return expandedReasoning.value.has(messageId);
}

// 滚动到底部
function scrollToBottom() {
  nextTick(() => {
    const container = messagesContainer.value;
    if (container) {
      container.scrollTop = container.scrollHeight;
    }
  });
}

// 生命周期
onMounted(() => {
  loadMessages();
  connectSSE();
});

onUnmounted(() => {
  if (eventSource) {
    eventSource.close();
  }
});
</script>

<style scoped>
.agent-terminal {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #1e1e1e;
  color: #d4d4d4;
}

.terminal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #2d2d30;
  border-bottom: 1px solid #3e3e42;
}

.status-badge {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
  margin-left: 12px;
}

.status-idle { background: #555; }
.status-running { background: #0e639c; animation: pulse 2s infinite; }
.status-awaiting_approval { background: #f39c12; }
.status-error { background: #e74c3c; }

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.message {
  margin-bottom: 16px;
}

.user-message, .ai-message {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.message-avatar {
  font-size: 24px;
  flex-shrink: 0;
}

.message-content {
  background: #2d2d30;
  padding: 12px 16px;
  border-radius: 8px;
  max-width: 80%;
}

.reasoning-section {
  background: #1e1e1e;
  padding: 8px;
  border-radius: 4px;
  margin-bottom: 8px;
}

.cursor-blink {
  animation: blink 1s step-start infinite;
}

@keyframes blink {
  50% { opacity: 0; }
}

.input-container {
  display: flex;
  gap: 8px;
  padding: 16px;
  background: #2d2d30;
  border-top: 1px solid #3e3e42;
}

textarea {
  flex: 1;
  padding: 12px;
  background: #1e1e1e;
  border: 1px solid #3e3e42;
  border-radius: 4px;
  color: #d4d4d4;
  font-family: 'Consolas', monospace;
  font-size: 14px;
  resize: none;
  min-height: 60px;
}

.send-btn {
  padding: 12px 24px;
  background: #0e639c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
}

.send-btn:hover {
  background: #1177bb;
}

.send-btn:disabled {
  background: #555;
  cursor: not-allowed;
}

.interrupt-btn {
  background: #e74c3c;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
}

.interrupt-btn:hover {
  background: #c0392b;
}
</style>
```

---

## 7. 工具系统增强

### 7.1 工具参数验证

**ToolValidationService.java**：
```java
@Service
public class ToolValidationService {
    
    public Map<String, Object> validateParams(String toolName, Map<String, Object> rawParams) throws ValidationException {
        switch (toolName) {
            case "execute_command":
                return validateExecuteCommand(rawParams);
            case "read_file":
                return validateReadFile(rawParams);
            case "write_file":
                return validateWriteFile(rawParams);
            case "edit_file":
                return validateEditFile(rawParams);
            case "search_files":
                return validateSearchFiles(rawParams);
            default:
                throw new ValidationException("Unknown tool: " + toolName);
        }
    }
    
    private Map<String, Object> validateExecuteCommand(Map<String, Object> rawParams) throws ValidationException {
        String command = validateString("command", rawParams.get("command"), true);
        String cwd = validateString("cwd", rawParams.get("cwd"), false);
        
        Map<String, Object> validated = new HashMap<>();
        validated.put("command", command);
        validated.put("cwd", cwd != null ? cwd : "/");
        return validated;
    }
    
    private Map<String, Object> validateReadFile(Map<String, Object> rawParams) throws ValidationException {
        String path = validateString("path", rawParams.get("path"), true);
        Integer startLine = validateInteger("start_line", rawParams.get("start_line"), false, 1, null);
        Integer endLine = validateInteger("end_line", rawParams.get("end_line"), false, 1, null);
        Integer pageNumber = validateInteger("page_number", rawParams.get("page_number"), false, 1, null);
        
        Map<String, Object> validated = new HashMap<>();
        validated.put("path", path);
        validated.put("startLine", startLine);
        validated.put("endLine", endLine);
        validated.put("pageNumber", pageNumber != null ? pageNumber : 1);
        return validated;
    }
    
    private String validateString(String fieldName, Object value, boolean required) throws ValidationException {
        if (value == null || value.toString().trim().isEmpty()) {
            if (required) {
                throw new ValidationException(String.format("Field '%s' is required but was null or empty", fieldName));
            }
            return null;
        }
        if (!(value instanceof String)) {
            throw new ValidationException(String.format("Field '%s' must be a string, but got %s", fieldName, value.getClass().getSimpleName()));
        }
        return value.toString();
    }
    
    private Integer validateInteger(String fieldName, Object value, boolean required, Integer min, Integer max) throws ValidationException {
        if (value == null) {
            if (required) {
                throw new ValidationException(String.format("Field '%s' is required but was null", fieldName));
            }
            return null;
        }
        
        Integer intValue;
        if (value instanceof Integer) {
            intValue = (Integer) value;
        } else if (value instanceof String) {
            try {
                intValue = Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                throw new ValidationException(String.format("Field '%s' must be an integer, but got '%s'", fieldName, value));
            }
        } else {
            throw new ValidationException(String.format("Field '%s' must be an integer, but got %s", fieldName, value.getClass().getSimpleName()));
        }
        
        if (min != null && intValue < min) {
            throw new ValidationException(String.format("Field '%s' must be >= %d, but got %d", fieldName, min, intValue));
        }
        if (max != null && intValue > max) {
            throw new ValidationException(String.format("Field '%s' must be <= %d, but got %d", fieldName, max, intValue));
        }
        
        return intValue;
    }
    
    public String formatResult(String toolName, ToolResult result) {
        if (!result.isSuccess()) {
            return String.format("[Error] %s", result.getErrorMessage());
        }
        
        switch (toolName) {
            case "execute_command":
                return formatExecuteCommandResult(result);
            case "read_file":
                return formatReadFileResult(result);
            case "write_file":
                return formatWriteFileResult(result);
            default:
                return result.getContent();
        }
    }
    
    private String formatExecuteCommandResult(ToolResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("命令执行成功\n\n");
        
        if (result.getStdout() != null && !result.getStdout().isEmpty()) {
            sb.append("输出：\n```\n");
            sb.append(result.getStdout());
            sb.append("\n```\n");
        }
        
        if (result.getStderr() != null && !result.getStderr().isEmpty()) {
            sb.append("\n错误输出：\n```\n");
            sb.append(result.getStderr());
            sb.append("\n```\n");
        }
        
        sb.append(String.format("\n退出码：%d", result.getExitCode()));
        
        return sb.toString();
    }
    
    private String formatReadFileResult(ToolResult result) {
        return String.format("%s\n```\n%s\n```", result.getFilePath(), result.getContent());
    }
    
    private String formatWriteFileResult(ToolResult result) {
        return String.format("文件写入成功：%s", result.getFilePath());
    }
}
```

---

## 8. 检查点系统实现

（见第 5.1.2 节 CheckpointService）

---

## 9. 批准机制设计

（见第 5.1.1 节 AgentLoopService 和第 4.1.2 节数据库设计）

---

## 10. 中断机制优化

**中断流程**：
1. 前端发送中断请求 → `/api/agent/interrupt`
2. 后端设置 `StreamState.interruptRequested = true`
3. Agent 循环在每次迭代前检查中断标志
4. 如果被中断，立即退出循环并清理状态

---

## 11. 实施路线图

### Phase 1：数据库与基础设施（Week 1-2）
- [ ] 创建 `chat_checkpoints` 表
- [ ] 创建 `tool_approvals` 表
- [ ] 创建 `user_approval_settings` 表
- [ ] 扩展 `chat_records` 表（添加 `checkpoint_id`, `loop_id`, `tool_approval_id`）
- [ ] 配置 Redis（SessionState 存储）

### Phase 2：核心服务开发（Week 3-4）
- [ ] 实现 `SessionStateService`
- [ ] 实现 `AgentLoopService`
- [ ] 实现 `CheckpointService`
- [ ] 实现 `ToolValidationService`
- [ ] 实现 `ToolApprovalService`

### Phase 3：Controller 层（Week 5）
- [ ] 重构 `AgentController`（替代 `TerminalController`）
- [ ] 添加批准/拒绝接口
- [ ] 添加中断接口
- [ ] 添加跳转检查点接口

### Phase 4：前端重构（Week 6-7）
- [ ] 重构 `AgentTerminalView.vue`
- [ ] 实现 `ToolApprovalDialog.vue`
- [ ] 实现 `CheckpointTimeline.vue`
- [ ] 实现 `SettingsDialog.vue`
- [ ] 优化 SSE 连接逻辑

### Phase 5：测试与优化（Week 8）
- [ ] 单元测试（Service 层）
- [ ] 集成测试（Controller 层）
- [ ] 端到端测试（前端 + 后端）
- [ ] 性能优化（Redis 缓存、数据库索引）

### Phase 6：文档与部署（Week 9-10）
- [ ] API 文档（Swagger）
- [ ] 用户手册
- [ ] 部署脚本
- [ ] 数据迁移工具

---

## 12. 测试策略

### 12.1 单元测试

**CheckpointServiceTest.java**：
```java
@SpringBootTest
public class CheckpointServiceTest {
    
    @Autowired
    private CheckpointService checkpointService;
    
    @Autowired
    private ChatRecordService chatRecordService;
    
    @Test
    public void testCreateCheckpoint() {
        // Given
        String sessionId = "test-session";
        Long userId = 1L;
        
        // When
        String checkpointId = checkpointService.createCheckpoint(sessionId, userId, CheckpointType.USER_MESSAGE, 1);
        
        // Then
        assertNotNull(checkpointId);
        Checkpoint checkpoint = checkpointRepository.findByCheckpointId(checkpointId).orElse(null);
        assertNotNull(checkpoint);
        assertEquals(sessionId, checkpoint.getSessionId());
    }
    
    @Test
    public void testJumpToCheckpoint_Undo() {
        // Given: 创建两个检查点
        String sessionId = "test-session";
        Long userId = 1L;
        String checkpoint1 = checkpointService.createCheckpoint(sessionId, userId, CheckpointType.USER_MESSAGE, 1);
        String checkpoint2 = checkpointService.createCheckpoint(sessionId, userId, CheckpointType.USER_MESSAGE, 2);
        
        // When: 跳转到 checkpoint1（撤销）
        checkpointService.jumpToCheckpoint(sessionId, userId, checkpoint1, false);
        
        // Then: 验证文件状态被恢复
        // ...
    }
}
```

### 12.2 集成测试

**AgentControllerIntegrationTest.java**：
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class AgentControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @WithMockUser(username = "testuser", authorities = {"USER"})
    public void testChatStream() throws Exception {
        // Given
        Map<String, Object> request = Map.of(
            "sessionId", "test-session",
            "message", "列出当前目录的文件"
        );
        
        // When
        MvcResult result = mockMvc.perform(post("/api/agent/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        
        // Then
        String response = result.getResponse().getContentAsString();
        assertNotNull(response);
    }
}
```

---

## 13. 风险评估与缓解

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| **数据库迁移失败** | 高 | 中 | 提供回滚脚本，先在测试环境验证 |
| **Redis 单点故障** | 高 | 低 | 配置 Redis 主从复制 + Sentinel |
| **SSE 连接不稳定** | 中 | 中 | 实现自动重连 + 心跳检测 |
| **检查点数据量过大** | 中 | 高 | 限制检查点保留数量（最多 50 个） |
| **Agent 循环死锁** | 高 | 低 | 设置最大循环次数 (10 次) + 超时机制 |
| **工具执行超时** | 中 | 中 | 为每个工具设置超时时间（30 秒） |
| **前端内存泄漏** | 中 | 中 | 定期清理不再使用的 SSE 监听器 |

---

**文档结束**

本重构指南详细阐述了 AISpring AI 终端系统的全面升级方案，涵盖了从数据库设计到前端重构的所有关键环节。通过借鉴 Void-Main 的优秀设计，AISpring 将拥有更强大、更安全、更易用的 AI Agent 系统。

