package com.aispring.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

/**
 * Redis缓存服务
 * 用于缓存聊天消息等热点数据
 */
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration CACHE_DURATION = Duration.ofHours(24);
    private static final String MESSAGES_CACHE_PREFIX = "chat:messages:";
    private static final String SESSION_CACHE_PREFIX = "chat:session:";

    /**
     * 缓存会话消息
     */
    public void cacheSessionMessages(String sessionId, Map<String, Object> messages) {
        try {
            String key = MESSAGES_CACHE_PREFIX + sessionId;
            String value = objectMapper.writeValueAsString(messages);
            redisTemplate.opsForValue().set(key, value, CACHE_DURATION);
        } catch (JsonProcessingException e) {
            // 序列化失败时不缓存，不影响主流程
            e.printStackTrace();
        }
    }

    /**
     * 获取缓存的会话消息
     */
    public Map<String, Object> getCachedSessionMessages(String sessionId) {
        try {
            String key = MESSAGES_CACHE_PREFIX + sessionId;
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                return objectMapper.readValue(value, Map.class);
            }
        } catch (JsonProcessingException e) {
            // 反序列化失败时返回null，不影响主流程
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 删除会话消息缓存
     */
    public void deleteSessionMessagesCache(String sessionId) {
        String key = MESSAGES_CACHE_PREFIX + sessionId;
        redisTemplate.delete(key);
    }

    /**
     * 缓存会话信息
     */
    public void cacheSessionInfo(String sessionId, Map<String, Object> sessionInfo) {
        try {
            String key = SESSION_CACHE_PREFIX + sessionId;
            String value = objectMapper.writeValueAsString(sessionInfo);
            redisTemplate.opsForValue().set(key, value, CACHE_DURATION);
        } catch (JsonProcessingException e) {
            // 序列化失败时不缓存，不影响主流程
            e.printStackTrace();
        }
    }

    /**
     * 获取缓存的会话信息
     */
    public Map<String, Object> getCachedSessionInfo(String sessionId) {
        try {
            String key = SESSION_CACHE_PREFIX + sessionId;
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                return objectMapper.readValue(value, Map.class);
            }
        } catch (JsonProcessingException e) {
            // 反序列化失败时返回null，不影响主流程
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 删除会话信息缓存
     */
    public void deleteSessionInfoCache(String sessionId) {
        String key = SESSION_CACHE_PREFIX + sessionId;
        redisTemplate.delete(key);
    }

    /**
     * 缓存分页会话消息
     */
    public void cachePagedSessionMessages(String sessionId, int page, int pageSize, Map<String, Object> messages) {
        try {
            String key = MESSAGES_CACHE_PREFIX + sessionId + ":" + page + ":" + pageSize;
            String value = objectMapper.writeValueAsString(messages);
            redisTemplate.opsForValue().set(key, value, CACHE_DURATION);
        } catch (JsonProcessingException e) {
            // 序列化失败时不缓存，不影响主流程
            e.printStackTrace();
        }
    }

    /**
     * 获取缓存的分页会话消息
     */
    public Map<String, Object> getCachedPagedSessionMessages(String sessionId, int page, int pageSize) {
        try {
            String key = MESSAGES_CACHE_PREFIX + sessionId + ":" + page + ":" + pageSize;
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                return objectMapper.readValue(value, Map.class);
            }
        } catch (JsonProcessingException e) {
            // 反序列化失败时返回null，不影响主流程
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 删除分页会话消息缓存
     */
    public void deletePagedSessionMessagesCache(String sessionId) {
        String pattern = MESSAGES_CACHE_PREFIX + sessionId + ":*";
        redisTemplate.delete(redisTemplate.keys(pattern));
    }
}