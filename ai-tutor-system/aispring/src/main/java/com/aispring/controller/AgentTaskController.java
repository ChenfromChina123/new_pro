package com.aispring.controller;

import com.aispring.controller.dto.AgentTaskCreateRequest;
import com.aispring.dto.response.ApiResponse;
import com.aispring.entity.AgentTask;
import com.aispring.security.CurrentUser;
import com.aispring.service.AgentSessionService;
import com.aispring.service.AgentTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Agent 任务控制器
 */
@RestController
@RequestMapping("/api/agent/tasks")
@Slf4j
@RequiredArgsConstructor
public class AgentTaskController {

    private final AgentTaskService taskService;
    private final AgentSessionService sessionService;

    /**
     * 创建任务
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AgentTask>> createTask(
            @CurrentUser Long userId,
            @Valid @RequestBody AgentTaskCreateRequest request) {
        log.info("Creating task for user: {}", userId);

        Long sessionId = request.getSessionId();
        if (sessionId == null) {
            sessionId = sessionService.getOrCreateActiveSession(userId).getId();
        }

        AgentTask task = taskService.createTask(
                sessionId,
                userId,
                request.getTaskType(),
                request.getInput()
        );
        return ResponseEntity.ok(ApiResponse.success(task));
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<AgentTask>> getTask(
            @CurrentUser Long userId,
            @PathVariable Long taskId) {
        return taskService.getTaskById(taskId, userId)
                .map(task -> ResponseEntity.ok(ApiResponse.success(task)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取会话的任务列表
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<List<AgentTask>>> getTasksBySession(
            @PathVariable Long sessionId) {
        List<AgentTask> tasks = taskService.getTasksBySessionId(sessionId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    /**
     * 分页获取会话的任务列表
     */
    @GetMapping("/session/{sessionId}/page")
    public ResponseEntity<ApiResponse<Page<AgentTask>>> getTasksBySessionPage(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AgentTask> tasks = taskService.getTasksBySessionId(sessionId, pageRequest);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    /**
     * 分页获取用户的任务列表
     */
    @GetMapping("/user/page")
    public ResponseEntity<ApiResponse<Page<AgentTask>>> getTasksByUserPage(
            @CurrentUser Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AgentTask> tasks = taskService.getTasksByUserId(userId, pageRequest);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    /**
     * 启动任务（流式响应）
     */
    @GetMapping(value = "/{taskId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter startTask(
            @CurrentUser Long userId,
            @PathVariable Long taskId) {
        log.info("Starting task stream for user: {}, task: {}", userId, taskId);
        return taskService.startTask(taskId, userId);
    }

    /**
     * 取消任务
     */
    @PostMapping("/{taskId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelTask(
            @CurrentUser Long userId,
            @PathVariable Long taskId) {
        taskService.cancelTask(taskId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 获取正在运行的任务
     */
    @GetMapping("/running/session/{sessionId}")
    public ResponseEntity<ApiResponse<List<AgentTask>>> getRunningTasks(
            @PathVariable Long sessionId) {
        List<AgentTask> tasks = taskService.getRunningTasks(sessionId);
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }
}
