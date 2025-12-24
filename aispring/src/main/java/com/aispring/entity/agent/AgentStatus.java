package com.aispring.entity.agent;

/**
 * Agent 状态枚举
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
public enum AgentStatus {
    /**
     * 空闲 - Agent 未执行任何任务
     */
    IDLE,
    
    /**
     * 规划中 - Agent 正在规划任务
     */
    PLANNING,
    
    /**
     * 运行中 - Agent 正在执行任务
     */
    RUNNING,
    
    /**
     * 等待工具 - Agent 正在等待工具执行结果
     */
    WAITING_TOOL,
    
    /**
     * 等待批准 - Agent 正在等待用户批准工具执行
     */
    AWAITING_APPROVAL,
    
    /**
     * 已暂停 - Agent 被用户暂停
     */
    PAUSED,
    
    /**
     * 已完成 - Agent 完成任务
     */
    COMPLETED,
    
    /**
     * 错误 - Agent 遇到错误
     */
    ERROR
}
