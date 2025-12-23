package com.aispring.service;

import com.aispring.entity.agent.AgentStatus;
import com.aispring.entity.agent.DecisionEnvelope;
import com.aispring.entity.agent.TaskState;
import com.aispring.entity.session.SessionState;
import com.aispring.entity.session.StreamState;

import java.util.Optional;

/**
 * 会话状态服务接口
 * 管理 Agent 循环的状态（存储在 Redis 中）
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
public interface SessionStateService {
    
    /**
     * 获取或创建会话状态
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 会话状态
     */
    SessionState getOrCreateState(String sessionId, Long userId);
    
    /**
     * 获取会话状态（如果不存在返回空）
     * 
     * @param sessionId 会话ID
     * @return 会话状态
     */
    Optional<SessionState> getState(String sessionId);
    
    /**
     * 保存会话状态
     * 
     * @param state 会话状态
     */
    void saveState(SessionState state);
    
    /**
     * 更新 Agent 状态
     * 
     * @param sessionId 会话ID
     * @param status Agent 状态
     */
    void updateAgentStatus(String sessionId, AgentStatus status);
    
    /**
     * 更新流式状态
     * 
     * @param sessionId 会话ID
     * @param streamState 流式状态
     */
    void updateStreamState(String sessionId, StreamState streamState);
    
    /**
     * 更新任务状态
     * 
     * @param sessionId 会话ID
     * @param taskState 任务状态
     */
    void updateTaskState(String sessionId, TaskState taskState);
    
    /**
     * 更新最后一次决策
     * 
     * @param sessionId 会话ID
     * @param decision 决策信封
     */
    void updateLastDecision(String sessionId, DecisionEnvelope decision);
    
    /**
     * 设置当前循环ID
     * 
     * @param sessionId 会话ID
     * @param loopId 循环ID
     */
    void setCurrentLoopId(String sessionId, String loopId);
    
    /**
     * 请求中断当前循环
     * 
     * @param sessionId 会话ID
     * @return 是否成功请求中断
     */
    boolean requestInterrupt(String sessionId);
    
    /**
     * 检查是否请求中断
     * 
     * @param sessionId 会话ID
     * @return 是否请求中断
     */
    boolean isInterruptRequested(String sessionId);
    
    /**
     * 清除中断标志
     * 
     * @param sessionId 会话ID
     */
    void clearInterrupt(String sessionId);
    
    /**
     * 删除会话状态
     * 
     * @param sessionId 会话ID
     */
    void deleteState(String sessionId);
    
    /**
     * 持久化会话状态到数据库（用于备份）
     * 
     * @param sessionId 会话ID
     */
    void persistStateToDatabase(String sessionId);
    
    /**
     * 从数据库恢复会话状态到 Redis
     * 
     * @param sessionId 会话ID
     * @return 是否成功恢复
     */
    boolean restoreStateFromDatabase(String sessionId);
}

