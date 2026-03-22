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
     * 配置 ObjectMapper 用于 JSON 序列化
     * - 支持 Java 8 时间类型
     * - 忽略 null 字段
     * - 格式化输出
     */
    private ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // 注册 Java 8 时间模块
        objectMapper.registerModule(new JavaTimeModule());
        
        // 禁用将日期写为时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 忽略 null 字段
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        
        // 忽略未知属性
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        return objectMapper;
    }
}

