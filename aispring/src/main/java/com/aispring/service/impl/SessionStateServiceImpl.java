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
    
    // Redis连接状态标记（避免重复日志）
    private volatile boolean redisAvailable = true;
    private volatile long lastRedisErrorLogTime = 0;
    private static final long REDIS_ERROR_LOG_INTERVAL = 60000; // 1分钟只记录一次Redis错误
    
    // 数据库持久化防抖：记录上次持久化时间
    private final Map<String, Long> lastPersistTime = new ConcurrentHashMap<>();
    private static final long PERSIST_DEBOUNCE_MS = 2000; // 2秒内只持久化一次
    
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
            redisAvailable = true; // 连接成功，重置标记
        } catch (Exception e) {
            redisAvailable = false;
            long now = System.currentTimeMillis();
            // 只在第一次失败或超过间隔时间后记录日志
            if (now - lastRedisErrorLogTime > REDIS_ERROR_LOG_INTERVAL) {
                log.warn("Redis 连接失败，使用本地缓存: {}", e.getMessage());
                lastRedisErrorLogTime = now;
            }
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
                redisAvailable = true;
                return Optional.of(state);
            }
            redisAvailable = true;
        } catch (Exception e) {
            redisAvailable = false;
            long now = System.currentTimeMillis();
            if (now - lastRedisErrorLogTime > REDIS_ERROR_LOG_INTERVAL) {
                log.warn("Redis 获取状态失败: {}", e.getMessage());
                lastRedisErrorLogTime = now;
            }
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
            redisAvailable = true; // 连接成功，重置标记
        } catch (Exception e) {
            redisAvailable = false;
            long now = System.currentTimeMillis();
            // 只在第一次失败或超过间隔时间后记录日志
            if (now - lastRedisErrorLogTime > REDIS_ERROR_LOG_INTERVAL) {
                log.warn("保存会话状态到 Redis 失败: {}", e.getMessage());
                lastRedisErrorLogTime = now;
            }
        }
        
        // 3. 防抖持久化到数据库（避免过于频繁的数据库操作）
        long now = System.currentTimeMillis();
        Long lastPersist = lastPersistTime.get(sessionId);
        // 只在关键状态变化或超过防抖时间时持久化
        boolean shouldPersist = lastPersist == null || 
                                (now - lastPersist > PERSIST_DEBOUNCE_MS) ||
                                state.getStatus() == AgentStatus.AWAITING_APPROVAL ||
                                state.getStatus() == AgentStatus.COMPLETED;
        
        if (shouldPersist) {
            lastPersistTime.put(sessionId, now);
            // 异步持久化，避免阻塞
            persistStateToDatabaseAsync(sessionId);
        }
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
            redisAvailable = true;
        } catch (Exception e) {
            redisAvailable = false;
            long now = System.currentTimeMillis();
            if (now - lastRedisErrorLogTime > REDIS_ERROR_LOG_INTERVAL) {
                log.warn("设置 Redis 中断标志失败: {}", e.getMessage());
                lastRedisErrorLogTime = now;
            }
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
            redisAvailable = true;
            if (Boolean.TRUE.equals(hasKey)) {
                return true;
            }
        } catch (Exception e) {
            redisAvailable = false;
            // Redis错误时不记录日志，避免日志过多
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
            redisAvailable = true;
        } catch (Exception e) {
            redisAvailable = false;
            // Redis错误时不记录日志，避免日志过多
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
        lastPersistTime.remove(sessionId);
        
        // 2. 清除 Redis 缓存
        try {
            sessionStateRedisTemplate.delete(key);
            sessionStateRedisTemplate.delete(interruptKey);
            redisAvailable = true;
        } catch (Exception e) {
            redisAvailable = false;
            // Redis错误时不记录日志，避免日志过多
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
        persistStateToDatabaseSync(sessionId);
    }
    
    /**
     * 同步持久化到数据库（用于关键操作）
     */
    private void persistStateToDatabaseSync(String sessionId) {
        SessionState state = localCache.get(sessionId);
        if (state == null) {
            state = getState(sessionId).orElse(null);
        }
        
        if (state != null) {
            try {
                SessionStateEntity entity = sessionStateRepository.findBySessionId(sessionId)
                        .orElse(new SessionStateEntity());
                
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
                log.error("持久化会话状态到数据库失败: sessionId={}, error={}", sessionId, e.getMessage());
            }
        }
    }
    
    /**
     * 异步持久化到数据库（用于非关键操作，避免阻塞）
     */
    private void persistStateToDatabaseAsync(String sessionId) {
        // 使用简单的异步执行，避免阻塞主线程
        new Thread(() -> {
            try {
                persistStateToDatabaseSync(sessionId);
            } catch (Exception e) {
                log.error("异步持久化会话状态失败: sessionId={}", sessionId, e);
            }
        }, "StatePersist-" + sessionId).start();
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


