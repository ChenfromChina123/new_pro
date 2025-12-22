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
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/terminal")
@RequiredArgsConstructor
public class TerminalController {

    private final TerminalService terminalService;
    private final AiChatService aiChatService;
    private final ChatRecordService chatRecordService;
    private final AgentStateService agentStateService;
    private final AgentPromptBuilder agentPromptBuilder;
    private final StateMutator stateMutator;
    private final TaskCompiler taskCompiler;
    private final ObjectMapper objectMapper;

    // --- Prompts ---
    private static final String PLANNER_PROMPT = """
            # 角色
            你是资深项目规划师。你的目标是分析用户意图，并生成结构化的任务流水线（Task Pipeline）。

            # 输入
            用户意图：%s

            # 输出格式
            1. 首先，请用中文简要描述你的规划思路（一段话，用户可见）。
            2. 然后，严格在 ```json 代码块中输出 JSON 任务数组。
            
            JSON 数组格式要求：
            每个任务对象必须包含字段：name、goal（字段名必须使用英文 name/goal）。
            字段值（name/goal）必须使用中文表述，简洁明确、可执行。
            如需拆分步骤，可选字段 substeps（字段名保持英文），其中每个子步骤至少包含 goal（字段名英文，值中文）。

            示例：
            （你的规划思路...）
            ```json
            [
              {"name":"初始化","goal":"确认项目结构与入口页面","substeps":[{"goal":"打开项目并确认目录结构"}]},
              {"name":"实现功能","goal":"完成核心功能实现","substeps":[{"goal":"补齐接口联调与错误处理"}]},
              {"name":"验证交付","goal":"运行校验并确保主要流程可用"}
            ]
            ```

            # 约束
            - 必须包含 ```json 代码块。
            - 任务必须是工程可执行步骤，避免空泛表述。
            """;

    private static final String EXECUTOR_PROMPT = """
            # 角色
            你是自主工程执行 Agent。你的目标是完成“当前任务”。

            # 上下文
            %s

            # 可用工具（工具名必须保持英文，且严格按此调用）
            - execute_command(command)
            - read_file(path)
            - write_file(path, content)
            - ensure_file(path, content)

            # 协议
            1. 分析当前任务与世界状态。
            2. 决定下一步要做什么。
            3. 输出一个“决策信封”（Decision Envelope）的 JSON。

            # 输出格式（严格 JSON，仅输出一个对象，不要 Markdown）
            字段名必须使用英文/下划线形式（例如 decision_id、tool_call、params 等），不要使用中文字段名。

            工具调用示例：
            {
              "decision_id": "UUID",
              "type": "TOOL_CALL",
              "action": "ensure_file",
              "params": { "path": "src/App.vue", "content": "..." },
              "expectation": { "world_change": ["src/App.vue"] }
            }

            若任务已完成：
            {
              "decision_id": "UUID",
              "type": "TASK_COMPLETE",
              "action": "none",
              "params": {}
            }
            """;

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
        
        // 1. Load Agent State
        AgentState state = agentStateService.getAgentState(sessionId, userId);
        
        // 2. Input Guard
        if (state.getStatus() == AgentStatus.RUNNING || state.getStatus() == AgentStatus.WAITING_TOOL) {
            if (isControlCommand(request.getPrompt())) {
                handleControlCommand(state, request.getPrompt());
                return sendSystemMessage("Agent " + state.getStatus());
            } else if (request.getTool_result() == null) {
                // If it's a normal chat message but agent is running, reject it
                return sendSystemMessage("Agent 正在运行中，请等待或输入 pause 暂停。");
            }
        }

        // 3. Handle Tool Result (Feedback Loop)
        if (request.getTool_result() != null) {
             MutatorResult result = stateMutator.applyToolResult(state, request.getTool_result());
             if (!result.isAccepted()) {
                 return sendSystemMessage("工具结果被拒绝：" + result.getReason());
             }
             // If accepted, state is updated (e.g. back to RUNNING or ERROR)
             agentStateService.saveAgentState(state);
             
             // If ERROR, stop here? Or continue to let LLM handle error?
             // Usually we want LLM to see the error.
        }

        // 4. Construct System Prompt
        String systemPrompt;
        if (state.getStatus() == AgentStatus.IDLE || state.getTaskState().getPipelineId() == null) {
            // Planner Mode
            state.setStatus(AgentStatus.PLANNING);
            systemPrompt = String.format(PLANNER_PROMPT, request.getPrompt());
        } else {
            // Executor Mode
            state.setStatus(AgentStatus.RUNNING);
            String context = agentPromptBuilder.buildPromptContext(state);
            systemPrompt = String.format(EXECUTOR_PROMPT, context);
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
                (fullResponse) -> {
                    try {
                        // Parse Decision Envelope or Plan
                        String json = extractJson(fullResponse);
                        if (json != null) {
                            json = json.trim();
                            if (json.startsWith("[")) {
                                // It is a Plan!
                                try {
                                    TaskState taskState = taskCompiler.compile(json, "pipeline-" + System.currentTimeMillis());
                                    // Update state with new plan
                                    // Note: we need to ensure we don't overwrite existing state incorrectly
                                    // But here we are just setting the task state
                                    AgentState currentState = agentStateService.getAgentState(sessionId, userId);
                                    currentState.setTaskState(taskState);
                                    currentState.setStatus(AgentStatus.RUNNING);
                                    agentStateService.saveAgentState(currentState);
                                } catch (Exception e) {
                                    System.err.println("Failed to compile plan: " + e.getMessage());
                                }
                            } else {
                                // Try to parse as DecisionEnvelope
                                try {
                                    DecisionEnvelope decision = objectMapper.readValue(json, DecisionEnvelope.class);
                                    state.setLastDecision(decision);
                                    
                                    if ("TASK_COMPLETE".equals(decision.getType())) {
                                        // Maybe mark as COMPLETED immediately?
                                        // But we want to wait for frontend to display it.
                                        // Let's keep it simple: just save decision.
                                    } else {
                                        state.setStatus(AgentStatus.WAITING_TOOL);
                                    }
                                    agentStateService.saveAgentState(state);
                                } catch (Exception e) {
                                    // Not a valid decision envelope, maybe just chat
                                    // Ignore or log
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to capture decision: " + e.getMessage());
                    }
                }
        );
    }
    
    private String extractJson(String text) {
        // Try to find markdown code block first
        int codeBlockStart = text.indexOf("```json");
        if (codeBlockStart >= 0) {
            int jsonStart = codeBlockStart + 7; // Length of "```json"
            int codeBlockEnd = text.indexOf("```", jsonStart);
            if (codeBlockEnd > jsonStart) {
                return text.substring(jsonStart, codeBlockEnd).trim();
            }
        }
        
        // Fallback to finding { ... } or [ ... ]
        int startObj = text.indexOf("{");
        int startArr = text.indexOf("[");
        int start = -1;
        
        if (startObj >= 0 && startArr >= 0) {
            start = Math.min(startObj, startArr);
        } else if (startObj >= 0) {
            start = startObj;
        } else if (startArr >= 0) {
            start = startArr;
        }
        
        if (start >= 0) {
            int endObj = text.lastIndexOf("}");
            int endArr = text.lastIndexOf("]");
            int end = Math.max(endObj, endArr);
            
            if (end > start) {
                return text.substring(start, end + 1);
            }
        }
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

        chatRecordService.createChatRecord(
            content,
            senderType,
            currentUser.getUser().getId().toString(),
            sessionId,
            model,
            "completed",
            "terminal"
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
