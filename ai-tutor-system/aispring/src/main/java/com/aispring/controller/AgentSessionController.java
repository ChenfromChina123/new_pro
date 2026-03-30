package com.aispring.controller;

import com.aispring.controller.dto.AgentSessionCreateRequest;
import com.aispring.controller.dto.AgentSessionUpdateRequest;
import com.aispring.dto.response.ApiResponse;
import com.aispring.entity.AgentSession;
import com.aispring.security.CurrentUser;
import com.aispring.service.AgentSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent 会话控制器
 */
@RestController
@RequestMapping("/api/agent/sessions")
@Slf4j
@RequiredArgsConstructor
public class AgentSessionController {
    
    private final AgentSessionService sessionService;
    
    /**
     * 创建新会话
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AgentSession>> createSession(
            @CurrentUser Long userId,
            @Valid @RequestBody AgentSessionCreateRequest request) {
        log.info("Creating session for user: {}", userId);
        AgentSession session = sessionService.createSession(
                userId, 
                request.getName(), 
                request.getWorkingDirectory()
        );
        return ResponseEntity.ok(ApiResponse.success(session));
    }
    
    /**
     * 获取会话列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AgentSession>>> getSessions(
            @CurrentUser Long userId) {
        List<AgentSession> sessions = sessionService.getSessionsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }
    
    /**
     * 分页获取会话列表
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<Page<AgentSession>>> getSessionsPage(
            @CurrentUser Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AgentSession> sessions = sessionService.getSessionsByUserId(userId, pageRequest);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }
    
    /**
     * 获取活跃会话
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<AgentSession>>> getActiveSessions(
            @CurrentUser Long userId) {
        List<AgentSession> sessions = sessionService.getActiveSessions(userId);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }
    
    /**
     * 获取或创建活跃会话
     */
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<AgentSession>> getOrCreateCurrentSession(
            @CurrentUser Long userId) {
        AgentSession session = sessionService.getOrCreateActiveSession(userId);
        return ResponseEntity.ok(ApiResponse.success(session));
    }
    
    /**
     * 获取会话详情
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<AgentSession>> getSession(
            @CurrentUser Long userId,
            @PathVariable Long sessionId) {
        return sessionService.getSessionById(sessionId, userId)
                .map(session -> ResponseEntity.ok(ApiResponse.success(session)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 更新会话
     */
    @PutMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<AgentSession>> updateSession(
            @CurrentUser Long userId,
            @PathVariable Long sessionId,
            @Valid @RequestBody AgentSessionUpdateRequest request) {
        AgentSession session = sessionService.updateSession(sessionId, userId, request.getName());
        return ResponseEntity.ok(ApiResponse.success(session));
    }
    
    /**
     * 关闭会话
     */
    @PostMapping("/{sessionId}/close")
    public ResponseEntity<ApiResponse<Void>> closeSession(
            @CurrentUser Long userId,
            @PathVariable Long sessionId) {
        sessionService.closeSession(sessionId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    /**
     * 删除会话
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(
            @CurrentUser Long userId,
            @PathVariable Long sessionId) {
        sessionService.deleteSession(sessionId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
