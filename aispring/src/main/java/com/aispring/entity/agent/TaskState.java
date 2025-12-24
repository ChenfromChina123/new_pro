package com.aispring.entity.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 任务流水线状态
 * 用于跟踪多步骤任务的执行进度
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskState implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 流水线ID
     */
    private String pipelineId;
    
    /**
     * 流水线名称/描述
     */
    private String pipelineName;
    
    /**
     * 当前任务ID
     */
    private String currentTaskId;
    
    /**
     * 任务列表（使用独立的 Task 类）
     */
    private List<Task> tasks;
    
    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING,      // 等待执行
        IN_PROGRESS,  // 执行中
        COMPLETED,    // 已完成
        FAILED        // 失败
    }
}
