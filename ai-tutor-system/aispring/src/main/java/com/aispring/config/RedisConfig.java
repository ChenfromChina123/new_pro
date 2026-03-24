package com.aispring.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 缓存配置类
 * 开发环境使用内存缓存，生产环境使用 Redis
 * 
 * @author AISpring Team
 * @since 2025-12-23
 */
@Configuration
@EnableCaching
@Profile("!prod") // 非生产环境使用内存缓存
public class RedisConfig {
    
    /**
     * 内存缓存管理器
     * 用于开发环境，无需 Redis
     */
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "wordDict",
            "vocabulary",
            "sessionState"
        ));
        return cacheManager;
    }
    
    /**
     * 为开发环境提供一个 Mock 的 RedisTemplate，防止启动报错
     */
    @Bean
    public org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate() {
        org.springframework.data.redis.core.RedisTemplate<String, Object> template = new org.springframework.data.redis.core.RedisTemplate<>();
        // 这里只是为了满足依赖注入，在开发环境下不会真正连接 Redis
        template.setConnectionFactory(new org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory());
        return template;
    }

    /**
     * 为开发环境提供一个 Mock 的 StringRedisTemplate，防止启动报错
     */
    @Bean
    public org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate() {
        org.springframework.data.redis.core.StringRedisTemplate template = new org.springframework.data.redis.core.StringRedisTemplate();
        template.setConnectionFactory(new org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory());
        return template;
    }
    
    /**
     * 配置 ObjectMapper 用于 JSON 序列化
     * - 支持 Java 8 时间类型
     * - 忽略 null 字段
     * - 格式化输出
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        return objectMapper;
    }
}

