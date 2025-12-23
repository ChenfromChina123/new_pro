package com.aispring.controller;

import com.aispring.dto.request.TerminalCommandRequest;
import com.aispring.dto.response.ApiResponse;
import com.aispring.dto.response.TerminalCommandResponse;
import com.aispring.dto.response.TerminalFileDto;
import com.aispring.entity.ChatRecord;
import com.aispring.entity.ChatSession;
import com.aispring.entity.agent.*;
import com.aispring.security.CustomUserDetails;
import com.aispring.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/terminal")
@RequiredArgsConstructor
@Slf4j
public class TerminalController {

    private final TerminalService terminalService;
    private final AiChatService aiChatService;
    private final ChatRecordService chatRecordService;
    private final AgentStateService agentStateService;
    private final AgentPromptBuilder agentPromptBuilder;
    private final StateMutator stateMutator;
    private final TaskCompiler taskCompiler;
    private final ObjectMapper objectMapper;
    private final TerminalPromptManager promptManager;

    @Data
    public static class TerminalChatRequest {
        @NotBlank(message = "提示词不能为空")
        private String prompt;
        private String session_id;
        private String model;
        private List<Map<String, Object>> tasks; // Legacy support
        private ToolResult tool_result; // For feedback loop
    }

    @Data
    public static class TerminalWriteFileRequest {
        @NotBlank(message = "文件路径不能为空")
        private String path;
        private String content;
        private String cwd;
        private Boolean overwrite;
    }

    @PostMapping(value = "/chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter chatStream(@AuthenticationPrincipal CustomUserDetails currentUser,
                                 @Valid @RequestBody TerminalChatRequest request) {
        Long userId = currentUser.getUser().getId();
        String sessionId = request.getSession_id();
        
        log.info("=== Terminal Chat Stream Request ===");
        log.info("Session: {}, User: {}", sessionId, userId);
        log.info("Prompt: {}", request.getPrompt());
        log.info("Tool Result: {}", request.getTool_result() != null ? 
            "ExitCode=" + request.getTool_result().getExitCode() : "null");
        
        // 1. Load Agent State
        AgentState state = agentStateService.getAgentState(sessionId, userId);
        log.info("Current Agent Status: {}", state.getStatus());
        log.info("Has Task Pipeline: {}", state.getTaskState() != null);
        
        // 2. Input Guard - 修复：只在没有tool_result且不是控制命令时才拦截
        if (state.getStatus() == AgentStatus.RUNNING || state.getStatus() == AgentStatus.WAITING_TOOL) {
            if (isControlCommand(request.getPrompt())) {
                log.info("Control command received: {}", request.getPrompt());
                handleControlCommand(state, request.getPrompt());
                return sendSystemMessage("Agent " + state.getStatus());
            } else if (request.getTool_result() == null && (request.getPrompt() == null || request.getPrompt().trim().isEmpty())) {
                // 只有在既没有tool_result也没有有效prompt时才拦截
                log.warn("Agent busy, no tool result or valid prompt");
                return sendSystemMessage("Agent 正在运行中，请等待或输入 pause 暂停。");
            }
        }

        // 3. Handle Tool Result (Feedback Loop)
        if (request.getTool_result() != null) {
             log.info("Processing tool result...");
             log.info("Tool Result - ExitCode: {}, Stdout: {}, Stderr: {}", 
                 request.getTool_result().getExitCode(),
                 request.getTool_result().getStdout() != null ? request.getTool_result().getStdout().substring(0, Math.min(100, request.getTool_result().getStdout().length())) : "null",
                 request.getTool_result().getStderr() != null ? request.getTool_result().getStderr().substring(0, Math.min(100, request.getTool_result().getStderr().length())) : "null");
             
             MutatorResult result = stateMutator.applyToolResult(state, request.getTool_result());
             log.info("MutatorResult - Accepted: {}, NewStatus: {}", result.isAccepted(), result.getNewAgentStatus());
             
             if (!result.isAccepted()) {
                 log.warn("Tool result rejected: {}", result.getReason());
                 return sendSystemMessage("工具结果被拒绝：" + result.getReason());
             }
             agentStateService.saveAgentState(state);
             
             // Cursor 工作流程：工具执行后自动触发下一轮循环（无论成功或失败）
             // 修复：无论有无任务流水线，都要自动继续
             
             // 1. 如果有任务流水线，继续执行任务
             if (state.getTaskState() != null && state.getTaskState().getCurrentTaskId() != null) {
                 log.info("Task pipeline mode - continuing with task: {}", state.getTaskState().getCurrentTaskId());
                 String continuationPrompt = buildContinuationPrompt(state, request.getTool_result());
                 String context = agentPromptBuilder.buildPromptContext(state);
                 String systemPrompt = promptManager.getExecutorPrompt(context);
                 
                 state.setStatus(AgentStatus.RUNNING);
                 agentStateService.saveAgentState(state);
                 
                 log.info("Triggering next round for task pipeline");
                 return aiChatService.askAgentStream(
                     continuationPrompt,
                     sessionId,
                     request.getModel(),
                     String.valueOf(userId),
                     systemPrompt,
                     null,
                     (fullResponse) -> handleAgentResponse(fullResponse, sessionId, userId)
                 );
             }
             // 2. 即时执行模式（EXECUTE），也要自动处理结果
             else {
                 log.info("Immediate execution mode - continuing after tool result");
                 String continuationPrompt = buildContinuationPromptForExecute(request.getTool_result());
                 String context = agentPromptBuilder.buildPromptContext(state);
                 String systemPrompt = promptManager.getExecutorPrompt(context);
                 
                 state.setStatus(AgentStatus.RUNNING);
                 agentStateService.saveAgentState(state);
                 
                 log.info("Triggering next round for immediate execution");
                 return aiChatService.askAgentStream(
                     continuationPrompt,
                     sessionId,
                     request.getModel(),
                     String.valueOf(userId),
                     systemPrompt,
                     null,
                     (fullResponse) -> handleAgentResponse(fullResponse, sessionId, userId)
                 );
             }
        }

        // 4. Construct System Prompt & Determine Role
        String systemPrompt;
        if (state.getTaskState() != null && state.getTaskState().getPipelineId() != null) {
            // Executor Mode - Already has a plan
            state.setStatus(AgentStatus.RUNNING);
            String context = agentPromptBuilder.buildPromptContext(state);
            systemPrompt = promptManager.getExecutorPrompt(context);
        } else {
            // IDLE or No Plan - Classify Intent
            String intent = aiChatService.ask(
                promptManager.getIntentClassifierPrompt(request.getPrompt()),
                null, request.getModel(), String.valueOf(userId)
            ).trim().toUpperCase();
            
            if (intent.contains("PLAN")) {
                state.setStatus(AgentStatus.PLANNING);
                systemPrompt = promptManager.getPlannerPrompt(request.getPrompt());
            } else if (intent.contains("EXECUTE")) {
                state.setStatus(AgentStatus.RUNNING);
                String context = agentPromptBuilder.buildPromptContext(state);
                systemPrompt = promptManager.getExecutorPrompt(context);
            } else {
                // Default to CHAT
                state.setStatus(AgentStatus.IDLE);
                String context = agentPromptBuilder.buildPromptContext(state);
                systemPrompt = promptManager.getChatPrompt(context);
            }
        }
        agentStateService.saveAgentState(state);

        // 5. Stream Response with Hook
        return aiChatService.askAgentStream(
                request.getPrompt(),
                sessionId,
                request.getModel(),
                String.valueOf(userId),
                systemPrompt,
                null,
                (fullResponse) -> handleAgentResponse(fullResponse, sessionId, userId)
        );
    }
    
    /**
     * 处理 Agent 响应 - 提取决策信封或任务计划
     */
    private void handleAgentResponse(String fullResponse, String sessionId, @SuppressWarnings("unused") Long userId) {
        try {
            log.info("=== Handling Agent Response ===");
            log.info("Response length: {}", fullResponse != null ? fullResponse.length() : 0);
            
            // Parse Decision Envelope or Plan
            String json = extractJson(fullResponse);
            if (json != null) {
                json = json.trim();
                log.info("Extracted JSON: {}", json.substring(0, Math.min(200, json.length())));
                
                if (json.startsWith("[")) {
                    // It is a Plan!
                    log.info("Detected task plan");
                    try {
                        TaskState taskState = taskCompiler.compile(json, "pipeline-" + System.currentTimeMillis());
                        AgentState currentState = agentStateService.getAgentState(sessionId, userId);
                        currentState.setTaskState(taskState);
                        if (taskState.getTasks() != null && !taskState.getTasks().isEmpty()) {
                            currentState.getTaskState().setCurrentTaskId(taskState.getTasks().get(0).getId());
                            taskState.getTasks().get(0).setStatus(TaskStatus.IN_PROGRESS);
                        }
                        currentState.setStatus(AgentStatus.RUNNING);
                        agentStateService.saveAgentState(currentState);
                        
                        log.info("Plan compiled successfully. Tasks: {}, Current: {}", 
                            taskState.getTasks() != null ? taskState.getTasks().size() : 0,
                            currentState.getTaskState().getCurrentTaskId());
                    } catch (Exception e) {
                        log.error("Failed to compile plan: {}", e.getMessage(), e);
                    }
                } else {
                    // Try to parse as DecisionEnvelope
                    log.info("Attempting to parse as decision envelope");
                    try {
                        DecisionEnvelope decision = objectMapper.readValue(json, DecisionEnvelope.class);
                        
                        // 自动生成 decision_id（如果AI没有提供）
                        if (decision.getDecisionId() == null || decision.getDecisionId().trim().isEmpty()) {
                            decision.setDecisionId(java.util.UUID.randomUUID().toString());
                            log.info("Auto-generated decision_id: {}", decision.getDecisionId());
                        } else {
                            log.info("Using AI-provided decision_id: {}", decision.getDecisionId());
                        }
                        
                        AgentState currentState = agentStateService.getAgentState(sessionId, userId);
                        currentState.setLastDecision(decision);
                        
                        log.info("Decision - Type: {}, Action: {}", decision.getType(), decision.getAction());
                        
                        if ("TASK_COMPLETE".equals(decision.getType())) {
                            log.info("Task completion detected");
                            stateMutator.markTaskComplete(currentState);
                            if (currentState.getStatus() == AgentStatus.COMPLETED) {
                                log.info("All tasks completed for session: {}", sessionId);
                            } else {
                                currentState.setStatus(AgentStatus.RUNNING);
                                log.info("Moving to next task");
                            }
                        } else if ("TOOL_CALL".equals(decision.getType())) {
                            currentState.setStatus(AgentStatus.WAITING_TOOL);
                            log.info("Tool call detected, status set to WAITING_TOOL");
                        }
                        agentStateService.saveAgentState(currentState);
                        log.info("Agent state saved successfully");
                    } catch (Exception e) {
                        log.warn("Response is not a decision envelope: {}", e.getMessage());
                        log.debug("Full response: {}", fullResponse);
                    }
                }
            } else {
                log.warn("No JSON found in response");
            }
        } catch (Exception e) {
            log.error("Error in handleAgentResponse: {}", e.getMessage(), e);
        }
    }
    
    private void logError("Failed to capture decision: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 构建继续执行的 Prompt - 基于工具结果（任务模式）
     */
    private String buildContinuationPrompt(AgentState state, ToolResult toolResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("工具执行完成。");
        
        if (toolResult.getExitCode() == 0) {
            sb.append("执行成功。");
            if (toolResult.getStdout() != null && !toolResult.getStdout().isEmpty()) {
                sb.append("\n输出：").append(toolResult.getStdout());
            }
            if (toolResult.getArtifacts() != null && !toolResult.getArtifacts().isEmpty()) {
                sb.append("\n创建的文件：").append(String.join(", ", toolResult.getArtifacts()));
            }
        } else {
            sb.append("执行失败。");
            if (toolResult.getStderr() != null && !toolResult.getStderr().isEmpty()) {
                sb.append("\n错误：").append(toolResult.getStderr());
            }
        }
        
        // 添加当前任务信息
        if (state.getTaskState() != null && state.getTaskState().getCurrentTaskId() != null) {
            Task currentTask = state.getTaskState().getTasks().stream()
                    .filter(t -> t.getId().equals(state.getTaskState().getCurrentTaskId()))
                    .findFirst()
                    .orElse(null);
            if (currentTask != null) {
                sb.append("\n\n当前任务：").append(currentTask.getGoal());
                sb.append("\n请继续执行当前任务的下一个步骤，或如果任务已完成，请输出 TASK_COMPLETE 决策。");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 构建继续执行的 Prompt - 基于工具结果（即时执行模式）
     */
    private String buildContinuationPromptForExecute(ToolResult toolResult) {
        StringBuilder sb = new StringBuilder();
        
        if (toolResult.getExitCode() == 0) {
            sb.append("命令执行成功。\n");
            if (toolResult.getStdout() != null && !toolResult.getStdout().isEmpty()) {
                sb.append("输出结果：\n").append(toolResult.getStdout());
            }
            if (toolResult.getArtifacts() != null && !toolResult.getArtifacts().isEmpty()) {
                sb.append("\n已创建文件：").append(String.join(", ", toolResult.getArtifacts()));
            }
            sb.append("\n\n请分析执行结果并回答用户的问题。如果任务已完成，请输出 TASK_COMPLETE 决策。");
        } else {
            sb.append("命令执行失败。\n");
            if (toolResult.getStderr() != null && !toolResult.getStderr().isEmpty()) {
                sb.append("错误信息：\n").append(toolResult.getStderr());
            }
            sb.append("\n\n请分析错误原因，修正命令并重新执行，或向用户解释问题并提供解决方案。");
        }
        
        return sb.toString();
    }
    
    private String extractJson(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        // 方式1: 查找 ```json 代码块
        int codeBlockStart = text.indexOf("```json");
        if (codeBlockStart >= 0) {
            int jsonStart = codeBlockStart + 7; // Length of "```json"
            int codeBlockEnd = text.indexOf("```", jsonStart);
            if (codeBlockEnd > jsonStart) {
                String json = text.substring(jsonStart, codeBlockEnd).trim();
                if (!json.isEmpty()) {
                    return json;
                }
            }
        }
        
        // 方式2: 查找 ``` 代码块（可能是其他语言标记）
        codeBlockStart = text.indexOf("```");
        if (codeBlockStart >= 0) {
            int jsonStart = text.indexOf("\n", codeBlockStart);
            if (jsonStart < 0) jsonStart = codeBlockStart + 3;
            int codeBlockEnd = text.indexOf("```", jsonStart);
            if (codeBlockEnd > jsonStart) {
                String json = text.substring(jsonStart, codeBlockEnd).trim();
                // 检查是否是 JSON
                if (json.startsWith("{") || json.startsWith("[")) {
                    return json;
                }
            }
        }
        
        // 方式3: 查找第一个 { 到最后一个 }（用于对象）
        int startObj = text.indexOf("{");
        if (startObj >= 0) {
            int endObj = text.lastIndexOf("}");
            if (endObj > startObj) {
                String json = text.substring(startObj, endObj + 1).trim();
                // 验证是否是有效的 JSON（简单检查）
                if (json.startsWith("{") && json.endsWith("}")) {
                    return json;
                }
            }
        }
        
        // 方式4: 查找第一个 [ 到最后一个 ]（用于数组）
        int startArr = text.indexOf("[");
        if (startArr >= 0) {
            int endArr = text.lastIndexOf("]");
            if (endArr > startArr) {
                String json = text.substring(startArr, endArr + 1).trim();
                // 验证是否是有效的 JSON（简单检查）
                if (json.startsWith("[") && json.endsWith("]")) {
                    return json;
                }
            }
        }
        
        // 方式5: 如果整个文本看起来像 JSON
        String trimmed = text.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return trimmed;
        }
        
        log.warn("Failed to extract JSON from text: {}", text.length() > 200 ? text.substring(0, 200) + "..." : text);
        return null;
    }
    
    @PostMapping("/submit-plan")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TaskState> submitPlan(@AuthenticationPrincipal CustomUserDetails currentUser,
                                             @RequestBody Map<String, String> payload) {
        String sessionId = payload.get("session_id");
        String planJson = payload.get("plan_json");
        
        TaskState taskState = taskCompiler.compile(planJson, "pipeline-" + System.currentTimeMillis());
        agentStateService.updateTaskState(sessionId, taskState);
        agentStateService.updateAgentStatus(sessionId, AgentStatus.RUNNING);
        
        return ApiResponse.success(taskState);
    }
    
    @PostMapping("/report-tool-result")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<MutatorResult> reportToolResult(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                       @RequestBody ToolResult result) {
         // This endpoint is an alternative to sending tool_result via chat-stream
         Long userId = currentUser.getUser().getId();
         // We need session_id in ToolResult? 
         // ToolResult doesn't have session_id.
         // This endpoint is tricky if we don't know the session.
         // Assuming single active session or passed in param.
         // Let's stick to chat-stream for now as Frontend uses it.
         // Or update ToolResult to include session_id.
         return ApiResponse.success(MutatorResult.builder().accepted(true).build());
    }

    private boolean isControlCommand(String prompt) {
        return "pause".equalsIgnoreCase(prompt) || "stop".equalsIgnoreCase(prompt);
    }

    private void handleControlCommand(AgentState state, String prompt) {
        if ("pause".equalsIgnoreCase(prompt)) {
            state.setStatus(AgentStatus.PAUSED);
        } else if ("stop".equalsIgnoreCase(prompt)) {
            state.setStatus(AgentStatus.IDLE);
        }
        agentStateService.saveAgentState(state);
    }

    private SseEmitter sendSystemMessage(String message) {
        SseEmitter emitter = new SseEmitter();
        try {
            emitter.send(SseEmitter.event().data("{\"content\": \"[系统] " + message + "\"}"));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    @PostMapping("/execute")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TerminalCommandResponse> executeCommand(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                               @RequestBody TerminalCommandRequest request) {
        TerminalCommandResponse response = terminalService.executeCommand(currentUser.getUser().getId(), request.getCommand(), request.getCwd());
        if (request.getSessionId() != null && response.getCwd() != null) {
            chatRecordService.updateSessionCwd(request.getSessionId(), response.getCwd(), currentUser.getUser().getId().toString());
        }
        return ApiResponse.success(response);
    }

    @PostMapping("/write-file")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TerminalCommandResponse> writeFile(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                          @Valid @RequestBody TerminalWriteFileRequest request) {
        boolean overwrite = request.getOverwrite() != null && request.getOverwrite();
        TerminalCommandResponse response = terminalService.writeFile(
                currentUser.getUser().getId(),
                request.getPath(),
                request.getContent(),
                request.getCwd(),
                overwrite
        );
        return ApiResponse.success(response);
    }

    @GetMapping("/files")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<TerminalFileDto>> listFiles(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                      @RequestParam(required = false, defaultValue = "") String path) {
        List<TerminalFileDto> files = terminalService.listFiles(currentUser.getUser().getId(), path);
        return ApiResponse.success(files);
    }

    @GetMapping("/read-file")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> readFile(@AuthenticationPrincipal CustomUserDetails currentUser,
                                      @RequestParam String path) {
        String content = terminalService.readFile(currentUser.getUser().getId(), path);
        return ApiResponse.success(content);
    }

    @PostMapping("/new-session")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ChatSession> createNewSession(@AuthenticationPrincipal CustomUserDetails currentUser) {
        ChatSession session = chatRecordService.createTerminalSession(currentUser.getUser().getId().toString());
        agentStateService.initializeAgentState(session.getSessionId(), currentUser.getUser().getId());
        return ApiResponse.success(session);
    }

    @GetMapping("/sessions")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ChatSession>> getSessions(@AuthenticationPrincipal CustomUserDetails currentUser) {
        List<ChatSession> sessions = chatRecordService.getTerminalSessions(currentUser.getUser().getId().toString());
        return ApiResponse.success(sessions);
    }

    @GetMapping("/history/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<ChatRecord>> getHistory(@AuthenticationPrincipal CustomUserDetails currentUser,
                                                  @PathVariable String sessionId) {
        List<ChatRecord> history = chatRecordService.getSessionMessages(currentUser.getUser().getId().toString(), sessionId);
        return ApiResponse.success(history);
    }

    @PostMapping("/save-record")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> saveRecord(@AuthenticationPrincipal CustomUserDetails currentUser,
                                      @RequestBody Map<String, Object> request) {
        String sessionId = (String) request.get("session_id");
        String content = (String) request.get("content");
        Integer senderType = (Integer) request.get("sender_type"); 
        String model = (String) request.get("model");

        String reasoningContent = (String) request.get("reasoning_content");  // 终端可能也有思考内容
        chatRecordService.createChatRecord(
            content,
            senderType,
            currentUser.getUser().getId().toString(),
            sessionId,
            model,
            "completed",
            "terminal",
            reasoningContent
        );
        return ApiResponse.success(null);
    }

    @DeleteMapping("/sessions/{sessionId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deleteSession(@AuthenticationPrincipal CustomUserDetails currentUser,
                                         @PathVariable String sessionId) {
        chatRecordService.deleteSession(currentUser.getUser().getId().toString(), sessionId);
        return ApiResponse.success(null);
    }
}
