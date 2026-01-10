package com.aispring.controller;

import com.aispring.entity.ChatRecord;
import com.aispring.entity.ChatSession;
import com.aispring.service.ChatRecordService;
import com.aispring.dto.response.MessageResponse;
import com.aispring.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天记录控制器
 * 对应Python: chat_records.py
 */
@RestController
@RequestMapping("/api/chat-records")
@RequiredArgsConstructor
public class ChatRecordController {
    
    private final ChatRecordService chatRecordService;
    
    // DTO类
    @Data
    public static class ChatRecordCreateRequest {
        @NotBlank(message = "内容不能为空")
        private String content;
        
        @NotNull(message = "发送者类型不能为空")
        private Integer senderType; // 1=用户, 2=AI
        
        private String sessionId;
        private String aiModel;
    }
    
    @Data
    public static class SaveRecordRequest {
        @JsonAlias({"session_id", "sessionId"})
        private String sessionId;
        
        @JsonProperty("user_message")
        private String userMessage;
        
        @JsonProperty("ai_response")
        private String aiResponse;
        
        @JsonProperty("ai_reasoning")
        private String aiReasoning;  // AI 深度思考内容
        
        private String model;
        private String role; // "user" or "assistant"
        private String content;
        private Long timestamp;
    }
    
    @Data
    public static class SessionIdRequest {
        @JsonAlias({"session_id", "sessionId"})
        private String sessionId;
    }
    
    /**
     * 保存聊天记录
     * Python: POST /api/chat-records/save
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveChatRecord(
            @RequestBody SaveRecordRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        
        // 获取当前用户ID
        Long userId = customUserDetails.getUser().getId();
        
        String session = request.getSessionId();
        String model = request.getModel();
        
        if (request.getUserMessage() != null || request.getAiResponse() != null) {
            // 保存用户消息
            chatRecordService.createChatRecord(
                request.getUserMessage(),
                1,
                userId,
                session,
                model,
                "completed"
            );
            // 保存 AI 消息，包含 reasoning_content
            chatRecordService.createChatRecord(
                request.getAiResponse(),
                2,
                userId,
                session,
                model,
                "completed",
                "chat",
                request.getAiReasoning()  // 传递深度思考内容
            );
        } else {
            Integer senderType = (request.getRole() != null && request.getRole().equalsIgnoreCase("user")) ? 1 : 2;
            String content = request.getContent();
            String reasoningContent = senderType == 2 ? request.getAiReasoning() : null;
            chatRecordService.createChatRecord(
                content,
                senderType,
                userId,
                session,
                model,
                "completed",
                "chat",
                reasoningContent
            );
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "聊天记录保存成功");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取用户的所有聊天会话
     * Python: GET /api/chat-records/sessions
     */
    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> getChatSessions(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        
        Long userId = customUserDetails.getUser().getId();
        List<Map<String, Object>> sessions = chatRecordService.getUserSessions(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessions", sessions);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取特定会话的所有消息
     * Python: GET /api/chat-records/session/{session_id}
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionMessages(
            @PathVariable String sessionId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        
        Long userId = customUserDetails.getUser().getId();
        List<ChatRecord> messages = chatRecordService.getSessionMessages(userId, sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("messages", messages.stream().map(ChatRecord::toMap).toList());
        
        // 添加会话级别的建议问题
        chatRecordService.getChatSession(sessionId).ifPresent(session -> {
            response.put("suggestions", session.getSuggestions());
            response.put("title", session.getTitle());
        });
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 删除特定会话
     * Python: DELETE /api/chat-records/session/{session_id}
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<MessageResponse> deleteSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        
        Long userId = customUserDetails.getUser().getId();
        chatRecordService.deleteSession(userId, sessionId);
        
        return ResponseEntity.ok(
            MessageResponse.builder()
                .message("会话已删除")
                .build()
        );
    }
    
    /**
     * 创建新会话
     * Python: POST /api/chat-records/new-session
     */
    @PostMapping("/new-session")
    public ResponseEntity<Map<String, String>> createNewSession(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        
        Long userId = customUserDetails.getUser().getId();
        ChatSession session = chatRecordService.createChatSession(userId, "chat");
        
        Map<String, String> response = new HashMap<>();
        response.put("session_id", session.getSessionId());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 管理员：获取所有用户的聊天会话
     * Python: GET /api/chat-records/admin/sessions
     */
    @GetMapping("/admin/sessions")
    public ResponseEntity<Map<String, Object>> getAllSessions(
            @RequestParam(defaultValue = "0") Integer skip,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // TODO: 添加管理员权限验证
        
        List<Map<String, Object>> sessions = chatRecordService.getAllSessions(skip, limit, userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessions", sessions);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 管理员：获取指定用户特定会话的消息
     * Python: GET /api/chat-records/admin/user/{user_id}/session/{session_id}
     */
    @GetMapping("/admin/user/{userId}/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> getUserSessionMessages(
            @PathVariable Long userId,
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // TODO: 添加管理员权限验证
        
        List<Map<String, Object>> messages = chatRecordService.getUserSessionMessages(userId, sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("messages", messages);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 管理员：删除指定用户的会话
     * Python: DELETE /api/chat-records/admin/user/{user_id}/session/{session_id}
     */
    @DeleteMapping("/admin/user/{userId}/session/{sessionId}")
    public ResponseEntity<MessageResponse> deleteUserSession(
            @PathVariable Long userId,
            @PathVariable String sessionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // TODO: 添加管理员权限验证
        
        chatRecordService.deleteSession(userId, sessionId);
        
        return ResponseEntity.ok(
            MessageResponse.builder()
                .message("成功删除会话")
                .build()
        );
    }
    
    /**
     * 管理员：获取聊天统计信息
     * Python: GET /api/chat-records/admin/stats
     */
    @GetMapping("/admin/stats")
    public ResponseEntity<Map<String, Object>> getChatStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // TODO: 添加管理员权限验证
        
        Map<String, Object> stats = chatRecordService.getChatStats();
        return ResponseEntity.ok(stats);
    }
}

