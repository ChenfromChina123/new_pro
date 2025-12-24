package com.aispring.repository;

import com.aispring.entity.session.SessionStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 会话状态数据库访问接口
 *
 * @author AISpring Team
 * @since 2025-12-24
 */
@Repository
public interface SessionStateRepository extends JpaRepository<SessionStateEntity, Long> {
    
    /**
     * 根据会话ID查询状态
     * 
     * @param sessionId 会话ID
     * @return 会话状态实体
     */
    Optional<SessionStateEntity> findBySessionId(String sessionId);
    
    /**
     * 根据会话ID删除状态
     * 
     * @param sessionId 会话ID
     */
    void deleteBySessionId(String sessionId);
}
