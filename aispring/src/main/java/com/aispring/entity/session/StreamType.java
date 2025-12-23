package com.aispring.entity.session;

/**
 * 流式状态类型枚举
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
public enum StreamType {
    /**
     * 正在流式生成 LLM 响应
     */
    STREAMING_LLM,
    
    /**
     * 正在执行工具
     */
    RUNNING_TOOL,
    
    /**
     * 等待用户批准
     */
    AWAITING_USER,
    
    /**
     * 空闲
     */
    IDLE
}

