package com.aispring.service.impl;

import com.aispring.entity.agent.AgentStatus;
import com.aispring.entity.agent.DecisionEnvelope;
import com.aispring.entity.agent.TaskState;
import com.aispring.entity.session.SessionState;
import com.aispring.entity.session.StreamState;
import com.aispring.service.SessionStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * 会话状态服务实现
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionStateServiceImpl implements SessionStateService {
    
    private static final String SESSION_STATE_PREFIX = "session:state:";
    private static final String INTERRUPT_FLAG_PREFIX = "session:interrupt:";
    
    private final RedisTemplate<String, SessionState> sessionStateRedisTemplate;
    
    @Value("${spring.redis.session-state.ttl:86400}")
    private long sessionStateTtl;  // 默认 24 小时
    
    @Value("${spring.redis.session-state.inactive-ttl:3600}")
    private long inactiveSessionTtl;  // 默认 1 小时
    
    /**
     * 获取 Redis key
     */
    private String getStateKey(String sessionId) {
        return SESSION_STATE_PREFIX + sessionId;
    }
    
    /**
     * 获取中断标志 key
     */
    private String getInterruptKey(String sessionId) {
        return INTERRUPT_FLAG_PREFIX + sessionId;
    }
    
    @Override
    public SessionState getOrCreateState(String sessionId, Long userId) {
        String key = getStateKey(sessionId);
        SessionState state = sessionStateRedisTemplate.opsForValue().get(key);
        
        if (state == null) {
            log.info("创建新的会话状态: sessionId={}, userId={}", sessionId, userId);
            state = SessionState.newIdle(sessionId, userId);
            saveState(state);
        } else {
            // 更新活跃时间
            state.touch();
            sessionStateRedisTemplate.opsForValue().set(key, state, Duration.ofSeconds(sessionStateTtl));
        }
        
        return state;
    }
    
    @Override
    public Optional<SessionState> getState(String sessionId) {
        String key = getStateKey(sessionId);
        SessionState state = sessionStateRedisTemplate.opsForValue().get(key);
        return Optional.ofNullable(state);
    }
    
    @Override
    public void saveState(SessionState state) {
        if (state == null || state.getSessionId() == null) {
            log.warn("尝试保存空的会话状态");
            return;
        }
        
        state.touch();
        String key = getStateKey(state.getSessionId());
        
        // 根据状态决定 TTL
        long ttl = (state.getStatus() == AgentStatus.IDLE) ? inactiveSessionTtl : sessionStateTtl;
        
        sessionStateRedisTemplate.opsForValue().set(key, state, Duration.ofSeconds(ttl));
        log.debug("保存会话状态: sessionId={}, status={}, ttl={}s", 
                state.getSessionId(), state.getStatus(), ttl);
    }
    
    @Override
    public void updateAgentStatus(String sessionId, AgentStatus status) {
        getState(sessionId).ifPresent(state -> {
            state.setStatus(status);
            saveState(state);
            log.info("更新 Agent 状态: sessionId={}, status={}", sessionId, status);
        });
    }
    
    @Override
    public void updateStreamState(String sessionId, StreamState streamState) {
        getState(sessionId).ifPresent(state -> {
            state.setStreamState(streamState);
            saveState(state);
            log.debug("更新流式状态: sessionId={}, type={}", sessionId, streamState.getType());
        });
    }
    
    @Override
    public void updateTaskState(String sessionId, TaskState taskState) {
        getState(sessionId).ifPresent(state -> {
            state.setTaskState(taskState);
            saveState(state);
            log.info("更新任务状态: sessionId={}, pipelineId={}", sessionId, taskState.getPipelineId());
        });
    }
    
    @Override
    public void updateLastDecision(String sessionId, DecisionEnvelope decision) {
        getState(sessionId).ifPresent(state -> {
            state.setLastDecision(decision);
            saveState(state);
            log.info("更新最后决策: sessionId={}, decisionId={}, tool={}", 
                    sessionId, decision.getDecisionId(), decision.getToolName());
        });
    }
    
    @Override
    public void setCurrentLoopId(String sessionId, String loopId) {
        getState(sessionId).ifPresent(state -> {
            state.setCurrentLoopId(loopId);
            saveState(state);
            log.info("设置当前循环ID: sessionId={}, loopId={}", sessionId, loopId);
        });
    }
    
    @Override
    public boolean requestInterrupt(String sessionId) {
        String interruptKey = getInterruptKey(sessionId);
        
        // 设置中断标志（5分钟过期）
        sessionStateRedisTemplate.opsForValue().set(interruptKey, 
                SessionState.builder().sessionId(sessionId).build(), 
                Duration.ofMinutes(5));
        
        // 更新状态中的中断标志
        Optional<SessionState> stateOpt = getState(sessionId);
        if (stateOpt.isPresent()) {
            SessionState state = stateOpt.get();
            if (state.getStreamState() != null) {
                state.getStreamState().setInterruptRequested(true);
                saveState(state);
            }
            log.warn("请求中断 Agent 循环: sessionId={}, loopId={}", 
                    sessionId, state.getCurrentLoopId());
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean isInterruptRequested(String sessionId) {
        String interruptKey = getInterruptKey(sessionId);
        Boolean hasKey = sessionStateRedisTemplate.hasKey(interruptKey);
        
        // 也检查状态中的标志
        Optional<SessionState> stateOpt = getState(sessionId);
        if (stateOpt.isPresent()) {
            SessionState state = stateOpt.get();
            if (state.getStreamState() != null && state.getStreamState().isInterruptRequested()) {
                return true;
            }
        }
        
        return Boolean.TRUE.equals(hasKey);
    }
    
    @Override
    public void clearInterrupt(String sessionId) {
        String interruptKey = getInterruptKey(sessionId);
        sessionStateRedisTemplate.delete(interruptKey);
        
        // 清除状态中的标志
        getState(sessionId).ifPresent(state -> {
            if (state.getStreamState() != null) {
                state.getStreamState().setInterruptRequested(false);
                saveState(state);
            }
        });
        
        log.info("清除中断标志: sessionId={}", sessionId);
    }
    
    @Override
    public void deleteState(String sessionId) {
        String key = getStateKey(sessionId);
        String interruptKey = getInterruptKey(sessionId);
        
        sessionStateRedisTemplate.delete(key);
        sessionStateRedisTemplate.delete(interruptKey);
        
        log.info("删除会话状态: sessionId={}", sessionId);
    }
    
    @Override
    public void persistStateToDatabase(String sessionId) {
        // TODO: 实现持久化到 session_states 表
        // 这个功能可以在后续实现，用于灾难恢复
        log.debug("持久化会话状态到数据库: sessionId={}", sessionId);
    }
    
    @Override
    public boolean restoreStateFromDatabase(String sessionId) {
        // TODO: 实现从 session_states 表恢复
        // 这个功能可以在后续实现，用于灾难恢复
        log.debug("从数据库恢复会话状态: sessionId={}", sessionId);
        return false;
    }
}

