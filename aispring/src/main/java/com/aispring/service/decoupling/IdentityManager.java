package com.aispring.service.decoupling;

import com.aispring.service.decoupling.PromptCompiler.TaskRole;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 身份管理器 - 管理三层身份结构
 * 
 * 核心思想：
 * - Core Identity: 长期，进程级
 * - Task Identity: 任务级，一次任务
 * - Viewpoint Identity: 瞬时，每次调用都变
 */
@Component
@Data
public class IdentityManager {

    @Data
    public static class CoreIdentity {
        private String agentType;
        private String authority; // "suggest_only" | "execute"
        private String domain;
        private Instant createdAt = Instant.now();
    }

    @Data
    public static class TaskIdentity {
        private String taskId;
        private TaskRole role;
        private String goal;
        private Instant createdAt = Instant.now();
        private Instant completedAt;
    }

    @Data
    public static class ViewpointIdentity {
        private String file;
        private String symbol;
        private Integer cursorLine;
        private java.util.List<String> contextScope = new java.util.ArrayList<>();
    }

    private CoreIdentity coreIdentity;
    private TaskIdentity currentTask;
    private ViewpointIdentity currentViewpoint;

    public IdentityManager() {
        // 默认核心身份
        this.coreIdentity = new CoreIdentity();
        this.coreIdentity.setAgentType("ide_software_engineer");
        this.coreIdentity.setAuthority("suggest_only");
        this.coreIdentity.setDomain("software");
    }

    /**
     * 设置当前任务身份
     */
    public void setTask(TaskIdentity task) {
        this.currentTask = task;
    }

    /**
     * 设置当前视角身份
     */
    public void setViewpoint(ViewpointIdentity viewpoint) {
        this.currentViewpoint = viewpoint;
    }

    /**
     * 获取复合身份 - 用于构建上下文
     */
    public Map<String, Object> getCompositeIdentity() {
        Map<String, Object> composite = new HashMap<>();
        
        // Core Identity
        Map<String, Object> core = new HashMap<>();
        core.put("type", coreIdentity.getAgentType());
        core.put("authority", coreIdentity.getAuthority());
        core.put("domain", coreIdentity.getDomain());
        composite.put("core", core);

        // Task Identity
        Map<String, Object> task = new HashMap<>();
        if (currentTask != null) {
            task.put("id", currentTask.getTaskId());
            task.put("role", currentTask.getRole() != null ? currentTask.getRole().name() : null);
            task.put("goal", currentTask.getGoal());
        }
        composite.put("task", task);

        // Viewpoint Identity
        Map<String, Object> viewpoint = new HashMap<>();
        if (currentViewpoint != null) {
            viewpoint.put("file", currentViewpoint.getFile());
            viewpoint.put("symbol", currentViewpoint.getSymbol());
            viewpoint.put("line", currentViewpoint.getCursorLine());
        }
        composite.put("viewpoint", viewpoint);

        return composite;
    }
}

