package com.aispring.entity.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 工具调用 DTO
 * 用于在 Agent 和前端之间传递工具调用信息
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCallDto implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 工具调用ID
     */
    private String id;
    
    /**
     * 工具名称
     */
    private String name;
    
    /**
     * 工具参数
     */
    private Map<String, Object> parameters;
    
    /**
     * 工具调用状态
     */
    @Builder.Default
    private ToolCallStatus status = ToolCallStatus.PENDING;
    
    /**
     * 工具执行结果
     */
    private String result;
    
    /**
     * 工具执行错误信息
     */
    private String error;
    
    /**
     * 工具调用状态枚举
     */
    public enum ToolCallStatus {
        PENDING,           // 等待执行
        AWAITING_APPROVAL, // 等待批准
        RUNNING,           // 执行中
        COMPLETED,         // 已完成
        FAILED             // 失败
    }
}

