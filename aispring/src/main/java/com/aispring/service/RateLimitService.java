package com.aispring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    
    private static final String KEY_PREFIX = "chat_limit:";
    private static final int MAX_REQUESTS = 5;
    private static final Duration EXPIRATION = Duration.ofHours(24);

    /**
     * Check if the IP is allowed to make a request.
     * If allowed, increments the counter.
     * @param ip Client IP address
     * @return true if allowed, false if limit exceeded
     */
    public boolean checkAndIncrement(String ip) {
        String key = KEY_PREFIX + ip;
        
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            
            // If it's the first request (count == 1), set expiration
            if (count != null && count == 1) {
                redisTemplate.expire(key, EXPIRATION);
            }
            
            if (count != null && count > MAX_REQUESTS) {
                log.warn("Rate limit exceeded for IP: {} (Count: {})", ip, count);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("Error accessing Redis for rate limiting", e);
            // If Redis fails, allow the request to avoid blocking users due to system error
            // Or deny it? Safe failure usually means allow.
            return true;
        }
    }
    
    /**
     * Get remaining requests for an IP
     * @param ip Client IP address
     * @return remaining requests count
     */
    public int getRemainingRequests(String ip) {
        String key = KEY_PREFIX + ip;
        String val = redisTemplate.opsForValue().get(key);
        if (val == null) {
            return MAX_REQUESTS;
        }
        try {
            int used = Integer.parseInt(val);
            return Math.max(0, MAX_REQUESTS - used);
        } catch (NumberFormatException e) {
            return MAX_REQUESTS;
        }
    }
}
