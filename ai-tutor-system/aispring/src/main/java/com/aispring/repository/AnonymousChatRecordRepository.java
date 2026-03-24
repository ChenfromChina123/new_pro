package com.aispring.repository;

import com.aispring.entity.AnonymousChatRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 匿名聊天记录仓库
 */
@Repository
public interface AnonymousChatRecordRepository extends JpaRepository<AnonymousChatRecord, Long> {
    
    /**
     * 根据会话ID查询聊天记录（用于上下文构建）
     */
    List<AnonymousChatRecord> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    /**
     * 根据会话ID与IP查询聊天记录（用于匿名用户隔离）
     */
    List<AnonymousChatRecord> findBySessionIdAndIpAddressOrderByCreatedAtAsc(String sessionId, String ipAddress);

    List<AnonymousChatRecord> findBySessionIdOrderByCreatedAtDesc(String sessionId, Pageable pageable);

    List<AnonymousChatRecord> findBySessionIdAndIpAddressOrderByCreatedAtDesc(String sessionId, String ipAddress, Pageable pageable);
    
    /**
     * 根据会话ID分页查询聊天记录，按创建时间倒序
     */
    List<AnonymousChatRecord> findBySessionIdOrderByCreatedAtDesc(String sessionId, int offset, int limit);
    
    /**
     * 根据会话ID和IP地址分页查询聊天记录，按创建时间倒序
     */
    List<AnonymousChatRecord> findBySessionIdAndIpAddressOrderByCreatedAtDesc(String sessionId, String ipAddress, int offset, int limit);
    
    /**
     * 统计会话的消息数量
     */
    long countBySessionId(String sessionId);
    
    /**
     * 统计会话和IP地址的消息数量
     */
    long countBySessionIdAndIpAddress(String sessionId, String ipAddress);
    
    /**
     * 根据IP地址查询最近的聊天记录（可选，用于审计）
     */
    List<AnonymousChatRecord> findByIpAddressOrderByCreatedAtDesc(String ipAddress);
}
