package com.sensa.authenticationservice.util;

import com.sensa.authenticationservice.dto.RedisTokenDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtils {

    private final RedisTemplate<String, RedisTokenDto> redisTemplate;

    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);

    public void saveTokenToRedis(String username, String token) {
        String RedisKey = "user: " + username;
        RedisTokenDto redisTokenDto = new RedisTokenDto();
        redisTokenDto.setUsername(username);
        redisTokenDto.setToken(token);

        try {
            redisTemplate.opsForValue().set(RedisKey, redisTokenDto, TOKEN_TTL);
            log.info("✅ Token stored in Redis for user: {} with TTL: {}", username, TOKEN_TTL);
        } catch (DataAccessException ex) {
            log.error("❌ Unable to store token in Redis for user: {}", username, ex);
        }
    }

    public boolean isRedisAvailable() {
        try {
            String pingResponse = redisTemplate.getConnectionFactory().getConnection().ping();
            return "PONG".equalsIgnoreCase(pingResponse);
        } catch (Exception ex) {
            log.warn("⚠️ Redis is unavailable: {}", ex.getMessage());
            return false;
        }
    }
}
