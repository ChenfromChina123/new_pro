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
        
        // 1. Load Agent State
        AgentState state = agentStateService.getAgentState(sessionId, userId);
        
        // 2. Input Guard
        if (state.getStatus() == AgentStatus.RUNNING || state.getStatus() == AgentStatus.WAITING_TOOL) {
            if (isControlCommand(request.getPrompt())) {
                handleControlCommand(state, request.getPrompt());
                return sendSystemMessage("Agent " + state.getStatus());
            } else if (request.getTool_result() == null) {
                return sendSystemMessage("Agent 正在运行中，请等待或输入 pause 暂停。");
            }
        }

        // 3. Handle Tool Result (Feedback Loop)
        if (request.getTool_result() != null) {
             MutatorResult result = stateMutator.applyToolResult(state, request.getTool_result());
             if (!result.isAccepted()) {
                 return sendSystemMessage("工具结果被拒绝：" + result.getReason());
             }
             agentStateService.saveAgentState(state);
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
