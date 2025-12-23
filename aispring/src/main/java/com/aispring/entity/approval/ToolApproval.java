package com.aispring.entity.approval;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * 工具批准记录实体
 * 用于跟踪工具调用的批准状态
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
@Entity
@Table(name = "tool_approvals", indexes = {
    @Index(name = "idx_session_user", columnList = "sessionId,userId"),
    @Index(name = "idx_decision", columnList = "decisionId"),
    @Index(name = "idx_status", columnList = "approvalStatus"),
    @Index(name = "idx_tool_name", columnList = "toolName"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolApproval {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 会话ID
     */
    @Column(nullable = false, length = 100)
    private String sessionId;
    
    /**
     * 用户ID
     */
    @Column(nullable = false)
    private Long userId;
    
    /**
     * 工具名称
     */
    @Column(nullable = false, length = 100)
    private String toolName;
    
    /**
     * 工具参数（JSON格式）
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    private Map<String, Object> toolParams;
    
    /**
     * 决策ID（关联 DecisionEnvelope）
     */
    @Column(nullable = false, unique = true, length = 64)
    private String decisionId;
    
    /**
     * 批准状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;
    
    /**
     * 批准/拒绝原因
     */
    @Column(length = 500)
    private String approvalReason;
    
    /**
     * 创建时间
     */
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    /**
     * 批准/拒绝时间
     */
    private Instant approvedAt;
}

