package com.sensa.authenticationservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisHealthCheckService {
    private final StringRedisTemplate redisTemplate;

    public boolean isRedisAvailable() {
        try {
            redisTemplate.opsForValue().set("health_check", "ok");
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
