package com.aispring.entity.approval;

/**
 * 批准状态枚举
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
public enum ApprovalStatus {
    /**
     * 等待批准
     */
    PENDING,
    
    /**
     * 已批准
     */
    APPROVED,
    
    /**
     * 已拒绝
     */
    REJECTED
}

