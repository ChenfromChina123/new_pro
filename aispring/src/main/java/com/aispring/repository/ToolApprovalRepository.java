package com.aispring.repository;

import com.aispring.entity.approval.ApprovalStatus;
import com.aispring.entity.approval.ToolApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 工具批准 Repository
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
@Repository
public interface ToolApprovalRepository extends JpaRepository<ToolApproval, Long> {
    
    /**
     * 根据决策ID查找
     */
    Optional<ToolApproval> findByDecisionId(String decisionId);
    
    /**
     * 根据会话ID和状态查找批准记录
     */
    List<ToolApproval> findBySessionIdAndApprovalStatusOrderByCreatedAtDesc(
            String sessionId, ApprovalStatus status);
    
    /**
     * 根据会话ID查找所有待批准的记录
     */
    List<ToolApproval> findBySessionIdAndApprovalStatus(String sessionId, ApprovalStatus status);
    
    /**
     * 根据用户ID和状态查找批准记录
     */
    List<ToolApproval> findByUserIdAndApprovalStatusOrderByCreatedAtDesc(
            Long userId, ApprovalStatus status);
    
    /**
     * 统计会话的待批准数量
     */
    long countBySessionIdAndApprovalStatus(String sessionId, ApprovalStatus status);
    
    /**
     * 删除指定时间之前的记录
     */
    @Modifying
    void deleteByCreatedAtBefore(Instant cutoffTime);
}

