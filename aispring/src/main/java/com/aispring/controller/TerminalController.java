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
            # Role
            You are an expert Project Planner. Your goal is to analyze the user's intent and create a structured Task Pipeline.
            
            # Input
            User Intent: %s
            
            # Output Format
            Return a strictly valid JSON array of tasks. Each task must have a 'name' and 'goal'.
            Example:
            [
              {"name": "Init", "goal": "Initialize project structure"},
              {"name": "Dev", "goal": "Implement features", "substeps": [{"goal": "Setup Vue"}]}
            ]
            
            # Constraints
            - Do not include any markdown formatting (```json).
            - Focus on engineering steps.
            """;

    private static final String EXECUTOR_PROMPT = """
            # Role
            You are an Autonomous Engineering Agent. Your goal is to complete the Current Task.
            
            # Context
            %s
            
            # Available Tools
            - execute_command(command)
            - read_file(path)
            - write_file(path, content)
            - ensure_file(path, content)
            
            # Protocol
            1. Analyze the Current Task and World State.
            2. Decide the next step.
            3. Output a Decision Envelope in JSON.
            
            # Output Format (Strict JSON)
            {
              "decision_id": "UUID",
              "type": "TOOL_CALL",
              "action": "ensure_file",
              "params": { "path": "src/App.vue", "content": "..." },
              "expectation": { "world_change": ["src/App.vue"] }
            }
            
            OR if task is done:
            {
              "decision_id": "UUID",
              "type": "TASK_COMPLETE",
              "action": "none",
              "params": {}
            }
            """;

    @Data
    public static class TerminalChatRequest {
        @NotBlank(message = "Prompt cannot be empty")
        private String prompt;
        private String session_id;
        private String model;
        private List<Map<String, Object>> tasks; // Legacy support
        private ToolResult tool_result; // For feedback loop
    }

    @Data
    public static class TerminalWriteFileRequest {
        @NotBlank(message = "File path cannot be empty")
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
                return sendSystemMessage("Agent is RUNNING. Please wait or type 'pause'.");
            }
        }

        // 3. Handle Tool Result (Feedback Loop)
        if (request.getTool_result() != null) {
             MutatorResult result = stateMutator.applyToolResult(state, request.getTool_result());
             if (!result.isAccepted()) {
                 return sendSystemMessage("Tool Result Rejected: " + result.getReason());
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
                        // Parse Decision Envelope
                        String json = extractJson(fullResponse);
                        if (json != null) {
                            // Try to parse as DecisionEnvelope
                            try {
                                DecisionEnvelope decision = objectMapper.readValue(json, DecisionEnvelope.class);
                                state.setLastDecision(decision);
                                // Also handle TASK_COMPLETE here? 
                                // No, TASK_COMPLETE is a decision type. The mutator handles it when frontend reports back?
                                // Actually, for TASK_COMPLETE, frontend might not call tool?
                                // Protocol says: 1 Agent Turn = 1 Decision.
                                // If Decision is TASK_COMPLETE, frontend receives it.
                                // Does Frontend send back a result for TASK_COMPLETE?
                                // Usually yes, to confirm "I saw it".
                                // Or backend can handle it immediately?
                                // Better to let Frontend acknowledge it via next loop or stop.
                                
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
                    } catch (Exception e) {
                        System.err.println("Failed to capture decision: " + e.getMessage());
                    }
                }
        );
    }
    
    private String extractJson(String text) {
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
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
            emitter.send(SseEmitter.event().data("{\"content\": \"[SYSTEM] " + message + "\"}"));
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
