package com.aispring.repository;

import com.aispring.entity.checkpoint.ChatCheckpoint;
import com.aispring.entity.checkpoint.CheckpointType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 聊天检查点 Repository
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
@Repository
public interface ChatCheckpointRepository extends JpaRepository<ChatCheckpoint, Long> {
    
    /**
     * 根据检查点ID查找
     */
    Optional<ChatCheckpoint> findByCheckpointId(String checkpointId);
    
    /**
     * 根据会话ID和用户ID查找所有检查点（按创建时间排序）
     */
    List<ChatCheckpoint> findBySessionIdAndUserIdOrderByCreatedAtAsc(String sessionId, Long userId);
    
    /**
     * 根据会话ID和消息顺序查找检查点
     */
    Optional<ChatCheckpoint> findBySessionIdAndMessageOrder(String sessionId, Integer messageOrder);
    
    /**
     * 根据会话ID和检查点类型查找检查点
     */
    List<ChatCheckpoint> findBySessionIdAndCheckpointTypeOrderByCreatedAtDesc(
            String sessionId, CheckpointType checkpointType);
    
    /**
     * 统计会话的检查点数量
     */
    long countBySessionId(String sessionId);
    
    /**
     * 删除指定时间之前的检查点
     */
    @Modifying
    void deleteByCreatedAtBefore(Instant cutoffTime);
}

