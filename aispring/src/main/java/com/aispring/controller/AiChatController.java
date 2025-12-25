package com.aispring.controller;

import com.aispring.service.AiChatService;
import com.aispring.dto.response.ApiResponse;
import com.aispring.security.CustomUserDetails;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

/**
 * AI聊天控制器
 * 对应Python: app.py中的/api/ask-stream端点
 */
@RestController
@RequiredArgsConstructor
public class AiChatController {
    
    private final AiChatService aiChatService;
    
    @Data
    public static class AskRequest {
        @NotBlank(message = "提示词不能为空")
        private String prompt;
        
        private String session_id;
        
        private String model;
    }
    
    /**
     * AI问答流式接口
     * Python: POST /api/ask-stream
     */
    @PostMapping(value = "/api/ask-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(
            @Valid @RequestBody AskRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        
        Long userId = customUserDetails.getUser().getId();
        
        // 调用AI流式聊天服务
        return aiChatService.askStream(
                request.getPrompt(),
                request.getSession_id(),
                request.getModel(),
                userId);
    }
    
    /**
     * AI问答非流式接口
     * Python: POST /api/ask
     */
    @PostMapping("/api/ask")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ask(
            @Valid @RequestBody AskRequest request,
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        
        Long userId = customUserDetails.getUser().getId();
        String answer = aiChatService.ask(
                request.getPrompt(),
                request.getSession_id(),
                request.getModel(),
                userId);
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("answer", answer);
        
        return ResponseEntity.ok(ApiResponse.success(responseData));
    }
}
