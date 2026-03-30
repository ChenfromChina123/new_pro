package com.aispring.service.impl;

import com.aispring.entity.AgentSession;
import com.aispring.entity.AgentTask;
import com.aispring.exception.CustomException;
import com.aispring.repository.AgentSessionRepository;
import com.aispring.repository.AgentTaskRepository;
import com.aispring.service.AgentTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Agent 任务服务实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentTaskServiceImpl implements AgentTaskService {
    
    private final AgentTaskRepository taskRepository;
    private final AgentSessionRepository sessionRepository;
    
    private final ConcurrentHashMap<Long, SseEmitter> taskEmitters = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    @Override
    @Transactional
    public AgentTask createTask(Long sessionId, Long userId, String taskType, String input) {
        log.info("Creating task for session: {}, user: {}, type: {}", sessionId, userId, taskType);
        
        AgentSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Session not found"));
        
        if (!session.isActive()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Session is not active");
        }
        
        AgentTask task = new AgentTask();
        task.setSessionId(sessionId);
        task.setUserId(userId);
        task.setTaskType(taskType);
        task.setInput(input);
        task.setStatus("pending");
        task.setTotalSteps(0);
        task.setCurrentStep(0);
        task.setTokensUsed(0L);
        task.setExecutionTimeMs(0L);
        
        return taskRepository.save(task);
    }
    
    @Override
    public Optional<AgentTask> getTaskById(Long taskId, Long userId) {
        return taskRepository.findByIdAndUserId(taskId, userId);
    }
    
    @Override
    public List<AgentTask> getTasksBySessionId(Long sessionId) {
        return taskRepository.findBySessionIdOrderByCreatedAtDesc(sessionId);
    }
    
    @Override
    public Page<AgentTask> getTasksBySessionId(Long sessionId, Pageable pageable) {
        return taskRepository.findBySessionId(sessionId, pageable);
    }
    
    @Override
    public Page<AgentTask> getTasksByUserId(Long userId, Pageable pageable) {
        return taskRepository.findByUserId(userId, pageable);
    }
    
    @Override
    @Transactional
    public SseEmitter startTask(Long taskId, Long userId) {
        log.info("Starting task: {} for user: {}", taskId, userId);
        
        AgentTask task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Task not found"));
        
        if (!"pending".equals(task.getStatus())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Task is not in pending status");
        }
        
        task.start();
        taskRepository.save(task);
        
        SseEmitter emitter = new SseEmitter(300000L);
        taskEmitters.put(taskId, emitter);
        
        emitter.onCompletion(() -> taskEmitters.remove(taskId));
        emitter.onTimeout(() -> {
            taskEmitters.remove(taskId);
            cancelTask(taskId, userId);
        });
        emitter.onError(e -> taskEmitters.remove(taskId));
        
        executorService.submit(() -> executeTask(task, emitter));
        
        return emitter;
    }
    
    @Override
    @Transactional
    public void cancelTask(Long taskId, Long userId) {
        log.info("Cancelling task: {} for user: {}", taskId, userId);
        
        AgentTask task = taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Task not found"));
        
        if (!task.isCompleted()) {
            task.cancel();
            taskRepository.save(task);
            
            SseEmitter emitter = taskEmitters.remove(taskId);
            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("status")
                            .data("{\"status\":\"cancelled\"}"));
                    emitter.complete();
                } catch (IOException e) {
                    log.error("Error sending cancellation event", e);
                }
            }
        }
    }
    
    @Override
    public List<AgentTask> getRunningTasks(Long sessionId) {
        return taskRepository.findRunningTasksBySessionId(sessionId);
    }
    
    @Override
    @Transactional
    public void updateTaskProgress(Long taskId, int currentStep, int totalSteps) {
        taskRepository.findById(taskId).ifPresent(task -> {
            task.setCurrentStep(currentStep);
            task.setTotalSteps(totalSteps);
            taskRepository.save(task);
            
            sendProgressEvent(taskId, currentStep, totalSteps);
        });
    }
    
    @Override
    @Transactional
    public void completeTask(Long taskId, String output, long tokensUsed) {
        log.info("Completing task: {}", taskId);
        
        taskRepository.findById(taskId).ifPresent(task -> {
            task.complete(output);
            task.setTokensUsed(tokensUsed);
            taskRepository.save(task);
            
            SseEmitter emitter = taskEmitters.remove(taskId);
            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("complete")
                            .data(output != null ? output : ""));
                    emitter.complete();
                } catch (IOException e) {
                    log.error("Error sending completion event", e);
                }
            }
        });
    }
    
    @Override
    @Transactional
    public void failTask(Long taskId, String errorMessage) {
        log.error("Task failed: {}, error: {}", taskId, errorMessage);
        
        taskRepository.findById(taskId).ifPresent(task -> {
            task.fail(errorMessage);
            taskRepository.save(task);
            
            SseEmitter emitter = taskEmitters.remove(taskId);
            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("{\"error\":\"" + errorMessage.replace("\"", "\\\"") + "\"}"));
                    emitter.completeWithError(new RuntimeException(errorMessage));
                } catch (IOException e) {
                    log.error("Error sending error event", e);
                }
            }
        });
    }
    
    /**
     * 执行任务（异步）
     */
    private void executeTask(AgentTask task, SseEmitter emitter) {
        try {
            sendEvent(emitter, "start", "{\"taskId\":" + task.getId() + "}");
            
            Thread.sleep(1000);
            
            completeTask(task.getId(), "Task completed successfully", 100L);
        } catch (Exception e) {
            log.error("Error executing task: {}", task.getId(), e);
            failTask(task.getId(), e.getMessage());
        }
    }
    
    /**
     * 发送事件
     */
    private void sendEvent(SseEmitter emitter, String name, String data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException e) {
            log.error("Error sending event: {}", name, e);
        }
    }
    
    /**
     * 发送进度事件
     */
    private void sendProgressEvent(Long taskId, int currentStep, int totalSteps) {
        SseEmitter emitter = taskEmitters.get(taskId);
        if (emitter != null) {
            sendEvent(emitter, "progress", 
                    "{\"currentStep\":" + currentStep + ",\"totalSteps\":" + totalSteps + "}");
        }
    }
}
