package com.aispring.service.impl;

import com.aispring.entity.agent.AgentStatus;
import com.aispring.entity.agent.DecisionEnvelope;
import com.aispring.entity.agent.TaskState;
import com.aispring.entity.session.SessionState;
import com.aispring.entity.session.SessionStateEntity;
import com.aispring.entity.session.StreamState;
import com.aispring.repository.SessionStateRepository;
import com.aispring.service.SessionStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
    private final SessionStateRepository sessionStateRepository;
    
    // 本地缓存作为 Redis 不可用时的降级方案
    private final Map<String, SessionState> localCache = new ConcurrentHashMap<>();
    private final Map<String, Instant> localInterruptFlags = new ConcurrentHashMap<>();
    
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
        SessionState state = null;
        
        try {
            state = sessionStateRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.error("Redis 连接失败，使用本地缓存: {}", e.getMessage());
            state = localCache.get(sessionId);
        }
        
        if (state == null) {
            log.info("创建新的会话状态: sessionId={}, userId={}", sessionId, userId);
            state = SessionState.newIdle(sessionId, userId);
            saveState(state);
        } else {
            // 更新活跃时间
            state.touch();
            try {
                sessionStateRedisTemplate.opsForValue().set(key, state, Duration.ofSeconds(sessionStateTtl));
            } catch (Exception e) {
                log.warn("Redis 更新状态失败: {}", e.getMessage());
                localCache.put(sessionId, state);
            }
        }
        
        return state;
    }
    
    @Override
    public Optional<SessionState> getState(String sessionId) {
        String key = getStateKey(sessionId);
        
        // 1. 尝试从 Redis 获取
        try {
            SessionState state = sessionStateRedisTemplate.opsForValue().get(key);
            if (state != null) {
                return Optional.of(state);
            }
        } catch (Exception e) {
            log.error("Redis 获取状态失败: {}", e.getMessage());
        }
        
        // 2. 尝试从本地缓存获取
        SessionState localState = localCache.get(sessionId);
        if (localState != null) {
            return Optional.of(localState);
        }
        
        // 3. 尝试从数据库恢复
        if (restoreStateFromDatabase(sessionId)) {
            return Optional.ofNullable(localCache.get(sessionId));
        }
        
        return Optional.empty();
    }
    
    @Override
    @Transactional
    public void saveState(SessionState state) {
        if (state == null || state.getSessionId() == null) {
            log.warn("尝试保存空的会话状态");
            return;
        }
        
        state.touch();
        String sessionId = state.getSessionId();
        String key = getStateKey(sessionId);
        
        // 1. 始终保存到本地缓存
        localCache.put(sessionId, state);
        
        // 2. 尝试保存到 Redis
        try {
            long ttl = (state.getStatus() == AgentStatus.IDLE) ? inactiveSessionTtl : sessionStateTtl;
            sessionStateRedisTemplate.opsForValue().set(key, state, Duration.ofSeconds(ttl));
        } catch (Exception e) {
            log.warn("保存会话状态到 Redis 失败: {}", e.getMessage());
        }
        
        // 3. 定期或在关键状态时持久化到数据库
        // 这里简单处理，每次保存都异步持久化到数据库
        persistStateToDatabase(sessionId);
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
        
        // 设置本地中断标志（5分钟后过期）
        localInterruptFlags.put(sessionId, Instant.now().plus(Duration.ofMinutes(5)));
        
        try {
            // 设置 Redis 中断标志（5分钟过期）
            sessionStateRedisTemplate.opsForValue().set(interruptKey, 
                    SessionState.builder().sessionId(sessionId).build(), 
                    Duration.ofMinutes(5));
        } catch (Exception e) {
            log.warn("设置 Redis 中断标志失败: {}", e.getMessage());
        }
        
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
        // 1. 检查本地标志
        Instant expiry = localInterruptFlags.get(sessionId);
        if (expiry != null && Instant.now().isBefore(expiry)) {
            return true;
        }
        
        // 2. 检查 Redis 标志
        try {
            String interruptKey = getInterruptKey(sessionId);
            Boolean hasKey = sessionStateRedisTemplate.hasKey(interruptKey);
            if (Boolean.TRUE.equals(hasKey)) {
                return true;
            }
        } catch (Exception e) {
            log.warn("检查 Redis 中断标志失败: {}", e.getMessage());
        }
        
        // 3. 检查状态中的标志
        Optional<SessionState> stateOpt = getState(sessionId);
        if (stateOpt.isPresent()) {
            SessionState state = stateOpt.get();
            if (state.getStreamState() != null && state.getStreamState().isInterruptRequested()) {
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public void clearInterrupt(String sessionId) {
        // 1. 清除本地标志
        localInterruptFlags.remove(sessionId);
        
        // 2. 清除 Redis 标志
        try {
            String interruptKey = getInterruptKey(sessionId);
            sessionStateRedisTemplate.delete(interruptKey);
        } catch (Exception e) {
            log.warn("清除 Redis 中断标志失败: {}", e.getMessage());
        }
        
        // 3. 清除状态中的标志
        getState(sessionId).ifPresent(state -> {
            if (state.getStreamState() != null) {
                state.getStreamState().setInterruptRequested(false);
                saveState(state);
            }
        });
        
        log.info("清除中断标志: sessionId={}", sessionId);
    }
    
    @Override
    @Transactional
    public void deleteState(String sessionId) {
        String key = getStateKey(sessionId);
        String interruptKey = getInterruptKey(sessionId);
        
        // 1. 清除本地缓存
        localCache.remove(sessionId);
        localInterruptFlags.remove(sessionId);
        
        // 2. 清除 Redis 缓存
        try {
            sessionStateRedisTemplate.delete(key);
            sessionStateRedisTemplate.delete(interruptKey);
        } catch (Exception e) {
            log.warn("删除 Redis 会话状态失败: {}", e.getMessage());
        }
        
        // 3. 清除数据库状态
        try {
            sessionStateRepository.deleteBySessionId(sessionId);
        } catch (Exception e) {
            log.error("删除数据库会话状态失败: {}", e.getMessage());
        }
        
        log.info("删除会话状态: sessionId={}", sessionId);
    }
    
    @Override
    @Transactional
    public void persistStateToDatabase(String sessionId) {
        SessionState state = localCache.get(sessionId);
        if (state == null) {
            // 如果本地没有，尝试从 Redis 获取
            state = getState(sessionId).orElse(null);
        }
        
        if (state != null) {
            try {
                SessionStateEntity entity = sessionStateRepository.findBySessionId(sessionId)
                        .orElse(new SessionStateEntity());
                
                // 更新实体字段
                entity.setSessionId(state.getSessionId());
                entity.setUserId(state.getUserId());
                entity.setAgentStatus(state.getStatus());
                entity.setCurrentLoopId(state.getCurrentLoopId());
                entity.setStreamState(state.getStreamState());
                entity.setTaskState(state.getTaskState());
                entity.setLastDecision(state.getLastDecision());
                entity.setLastCheckpointId(state.getLastCheckpointId());
                entity.setUpdatedAt(state.getUpdatedAt());
                entity.setLastActiveAt(state.getUpdatedAt());
                
                sessionStateRepository.save(entity);
                log.debug("持久化会话状态到数据库成功: sessionId={}", sessionId);
            } catch (Exception e) {
                log.error("持久化会话状态到数据库失败: {}", e.getMessage());
            }
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean restoreStateFromDatabase(String sessionId) {
        try {
            Optional<SessionStateEntity> entityOpt = sessionStateRepository.findBySessionId(sessionId);
            if (entityOpt.isPresent()) {
                SessionState state = entityOpt.get().toPojo();
                localCache.put(sessionId, state);
                log.info("从数据库恢复会话状态成功: sessionId={}", sessionId);
                return true;
            }
        } catch (Exception e) {
            log.error("从数据库恢复会话状态失败: {}", e.getMessage());
        }
        return false;
    }
}

