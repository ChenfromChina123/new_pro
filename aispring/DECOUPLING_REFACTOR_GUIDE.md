# 解耦架构重构指南

本文档说明如何将新的 4 个解耦系统集成到现有的 Agent 系统中。

## 一、架构概览

新的解耦架构包含 4 个独立系统：

1. **提示词系统解耦** (`PromptCompiler`) - Prompt 是编译产物，不是状态
2. **工具系统解耦** (`CapabilityAdapter` + `InvocationPolicy`) - 模型只能提议，系统决定执行
3. **身份定位系统解耦** (`IdentityManager`) - 三层身份结构（核心/任务/视角）
4. **信息解耦系统** (`InformationManager`) - 状态切片，可重构

## 二、已创建的新组件

### 1. 解耦系统核心类

- `com.aispring.service.decoupling.PromptCompiler` - 提示词编译器
- `com.aispring.service.decoupling.CapabilityAdapter` - 能力适配器
- `com.aispring.service.decoupling.InvocationPolicy` - 调用策略
- `com.aispring.service.decoupling.IdentityManager` - 身份管理器
- `com.aispring.service.decoupling.InformationManager` - 信息管理器

### 2. 集成服务

- `com.aispring.service.DecoupledPromptService` - 使用解耦系统构建 Prompt 的服务

## 三、重构步骤

### Step 1: 更新 TerminalController

在 `TerminalController` 中，将原有的 Prompt 构建逻辑替换为使用 `DecoupledPromptService`：

**原有代码：**
```java
String context = agentPromptBuilder.buildPromptContext(state);
systemPrompt = promptManager.getExecutorPrompt(context);
```

**新代码：**
```java
systemPrompt = decoupledPromptService.buildExecutorPrompt(state, request.getPrompt());
```

### Step 2: 集成工具系统解耦

在 `TerminalController` 中，添加工具执行前的策略检查：

```java
@Autowired
private CapabilityAdapter capabilityAdapter;

@Autowired
private InvocationPolicy invocationPolicy;

// 在执行工具前
CapabilityAdapter.Capability capability = parseCapability(decision.getAction());
InvocationPolicy.PolicyResult policyResult = invocationPolicy.canExecute(capability);

if (!policyResult.isAllowed()) {
    return sendSystemMessage("工具执行被拒绝：" + policyResult.getReason());
}

// 执行工具
ToolResult result = capabilityAdapter.execute(capability, decision.getParams(), decision.getDecisionId());
```

### Step 3: 集成身份系统

在构建 Prompt 前，设置当前任务身份：

```java
@Autowired
private IdentityManager identityManager;

// 设置任务身份
if (state.getTaskState() != null) {
    IdentityManager.TaskIdentity taskIdentity = new IdentityManager.TaskIdentity();
    taskIdentity.setTaskId(state.getTaskState().getCurrentTaskId());
    taskIdentity.setRole(PromptCompiler.TaskRole.EXECUTOR);
    identityManager.setTask(taskIdentity);
}
```

### Step 4: 集成信息系统

信息系统的集成已经在 `DecoupledPromptService` 中完成，它会自动从 `AgentState` 构建状态切片。

## 四、完整示例

### 重构后的 TerminalController.chatStream 方法（部分）

```java
@PostMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
@PreAuthorize("isAuthenticated()")
public SseEmitter chatStream(@AuthenticationPrincipal CustomUserDetails currentUser,
                             @Valid @RequestBody TerminalChatRequest request) {
    Long userId = currentUser.getUser().getId();
    String sessionId = request.getSession_id();
    
    // 1. Load Agent State
    AgentState state = agentStateService.getAgentState(sessionId, userId);
    
    // 2. Input Guard (保持不变)
    if (state.getStatus() == AgentStatus.RUNNING || state.getStatus() == AgentStatus.WAITING_TOOL) {
        if (isControlCommand(request.getPrompt())) {
            handleControlCommand(state, request.getPrompt());
            return sendSystemMessage("Agent " + state.getStatus());
        } else if (request.getTool_result() == null) {
            return sendSystemMessage("Agent 正在运行中，请等待或输入 pause 暂停。");
        }
    }

    // 3. Handle Tool Result (保持不变)
    if (request.getTool_result() != null) {
         MutatorResult result = stateMutator.applyToolResult(state, request.getTool_result());
         if (!result.isAccepted()) {
             return sendSystemMessage("工具结果被拒绝：" + result.getReason());
         }
         agentStateService.saveAgentState(state);
    }

    // 4. 使用解耦系统构建 Prompt
    String systemPrompt;
    if (state.getTaskState() != null && state.getTaskState().getPipelineId() != null) {
        // Executor Mode
        state.setStatus(AgentStatus.RUNNING);
        systemPrompt = decoupledPromptService.buildExecutorPrompt(state, request.getPrompt());
    } else {
        // IDLE or No Plan - Classify Intent
        String intent = aiChatService.ask(
            promptManager.getIntentClassifierPrompt(request.getPrompt()),
            null, request.getModel(), String.valueOf(userId)
        ).trim().toUpperCase();
        
        if (intent.contains("PLAN")) {
            state.setStatus(AgentStatus.PLANNING);
            systemPrompt = decoupledPromptService.buildPlannerPrompt(request.getPrompt());
        } else if (intent.contains("EXECUTE")) {
            state.setStatus(AgentStatus.RUNNING);
            systemPrompt = decoupledPromptService.buildExecutorPrompt(state, request.getPrompt());
        } else {
            // Default to CHAT
            state.setStatus(AgentStatus.IDLE);
            systemPrompt = decoupledPromptService.buildChatPrompt(state, request.getPrompt());
        }
    }
    agentStateService.saveAgentState(state);

    // 5. Stream Response (保持不变)
    return aiChatService.askAgentStream(...);
}
```

## 五、优势对比

### 原有架构的问题

1. **Prompt 混在代码中** - 难以替换和测试
2. **工具执行无策略控制** - 模型可以直接执行
3. **身份信息不明确** - 混在 Prompt 中
4. **信息管理混乱** - 直接传递整个状态

### 新架构的优势

1. **Prompt 可替换** - 通过配置编译，易于测试和替换
2. **工具执行可控** - 策略层控制，模型只能提议
3. **身份清晰分层** - 核心/任务/视角三层结构
4. **信息可重构** - 状态切片，按需过滤

## 六、测试验证

运行 Python 测试脚本验证解耦系统：

```bash
python tools/test_decoupling_systems.py
```

所有测试应该通过，验证：
- ✅ Prompt 是编译产物，不是状态
- ✅ 模型只能提议，系统决定执行
- ✅ 身份分为三层：核心/任务/视角
- ✅ 信息是状态切片，可重构
- ✅ 4 个系统完全解耦，可独立替换

## 七、迁移建议

1. **渐进式迁移** - 先在新功能中使用，逐步替换旧代码
2. **保持兼容** - 保留原有 `TerminalPromptManager` 和 `AgentPromptBuilder`，避免破坏现有功能
3. **充分测试** - 在测试环境验证新系统后再部署到生产环境

## 八、下一步

1. 更新 `TerminalController` 使用 `DecoupledPromptService`
2. 在工具执行前添加策略检查
3. 集成身份管理和信息管理
4. 编写单元测试验证解耦系统
5. 更新前端以适配新的架构（如果需要）

