package com.aispring.repository;

import com.aispring.entity.ChatRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聊天记录仓库接口
 * 对应Python: ChatRecord模型的数据库操作
 */
@Repository
public interface ChatRecordRepository extends JpaRepository<ChatRecord, Long> {
    
    /**
     * 根据用户ID查找所有聊天记录
     */
    List<ChatRecord> findByUserIdOrderByMessageOrderAsc(String userId);
    
    /**
     * 根据用户ID和会话ID查找聊天记录
     */
    List<ChatRecord> findByUserIdAndSessionIdOrderByMessageOrderAsc(String userId, String sessionId);
    
    /**
     * 根据会话ID和用户ID删除聊天记录
     */
    void deleteByUserIdAndSessionId(String userId, String sessionId);
    
    /**
     * 查询用户的所有会话ID（去重）
     */
    @Query("SELECT DISTINCT c.sessionId FROM ChatRecord c WHERE c.userId = :userId ORDER BY MAX(c.sendTime) DESC")
    List<String> findDistinctSessionIdsByUserId(@Param("userId") String userId);
    
    /**
     * 获取会话的最新消息顺序号
     */
    @Query("SELECT COALESCE(MAX(c.messageOrder), 0) FROM ChatRecord c WHERE c.sessionId = :sessionId AND c.userId = :userId")
    Integer findMaxMessageOrderBySessionIdAndUserId(@Param("sessionId") String sessionId, @Param("userId") String userId);
    
    /**
     * 获取用户所有会话的基本信息
     */
    @Query(value = "SELECT c.session_id, MAX(c.send_time) AS last_message_time, " +
           "(SELECT c2.content FROM chat_records c2 WHERE c2.session_id = c.session_id AND c2.user_id = c.user_id " +
           "ORDER BY c2.send_time DESC LIMIT 1) AS last_message " +
           "FROM chat_records c WHERE c.user_id = :userId " +
           "GROUP BY c.session_id ORDER BY last_message_time DESC", nativeQuery = true)
    List<Object[]> findSessionInfoByUserId(@Param("userId") String userId);
    
    /**
     * 管理员：获取所有会话信息（带分页）
     */
    @Query(value = "SELECT c.session_id, c.user_id, MAX(c.send_time) AS last_message_time, " +
           "COUNT(c.id) AS message_count, " +
           "(SELECT c2.content FROM chat_records c2 WHERE c2.session_id = c.session_id " +
           "ORDER BY c2.send_time DESC LIMIT 1) AS last_message " +
           "FROM chat_records c GROUP BY c.session_id, c.user_id ORDER BY last_message_time DESC", nativeQuery = true)
    List<Object[]> findAllSessionsInfo();
    
    /**
     * 管理员：根据用户ID筛选会话信息
     */
    @Query(value = "SELECT c.session_id, c.user_id, MAX(c.send_time) AS last_message_time, " +
           "COUNT(c.id) AS message_count, " +
           "(SELECT c2.content FROM chat_records c2 WHERE c2.session_id = c.session_id " +
           "ORDER BY c2.send_time DESC LIMIT 1) AS last_message " +
           "FROM chat_records c WHERE c.user_id = :userId " +
           "GROUP BY c.session_id, c.user_id ORDER BY last_message_time DESC", nativeQuery = true)
    List<Object[]> findSessionsInfoByUserId(@Param("userId") String userId);
    
    /**
     * 统计总消息数
     */
    @Query("SELECT COUNT(c) FROM ChatRecord c")
    Long countTotalMessages();
    
    /**
     * 统计总会话数
     */
    @Query("SELECT COUNT(DISTINCT c.sessionId) FROM ChatRecord c")
    Long countTotalSessions();
    
    /**
     * 统计活跃用户数
     */
    @Query("SELECT COUNT(DISTINCT c.userId) FROM ChatRecord c")
    Long countActiveUsers();
    
    /**
     * 统计今日新增消息数
     */
    @Query("SELECT COUNT(c) FROM ChatRecord c WHERE CAST(c.sendTime AS date) = CURRENT_DATE")
    Long countTodayMessages();
}

