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

    public void deleteTokenFromRedis(String username) {
        String redisKey = "user: " + username;
        try {
            redisTemplate.delete(redisKey);
            log.info("🗑️ Token deleted from Redis for user: {}", username);
        } catch (DataAccessException ex) {
            log.error("❌ Unable to delete token fro Redis for user: {}", username, ex);
        }
    }
}
