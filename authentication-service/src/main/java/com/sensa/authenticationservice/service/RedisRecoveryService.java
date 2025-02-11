package com.sensa.authenticationservice.service;

import com.sensa.authenticationservice.dto.RedisBackupDto;
import com.sensa.authenticationservice.dto.RedisTokenDto;
import com.sensa.authenticationservice.entity.RedisBackupEntity;
import com.sensa.authenticationservice.mapper.RedisBackupEntityMapper;
import com.sensa.authenticationservice.repository.RedisBackupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRecoveryService {

    private final RedisBackupRepository redisBackupRepository;
    private final RedisTemplate<String, RedisTokenDto> redisTemplate;

    private volatile boolean redisAvailable = true;

    public void saveToken(String username, String token) {
        String redisKey = "user: " + username;
        RedisTokenDto redisTokenDto = new RedisTokenDto();
        redisTokenDto.setUsername(username);
        redisTokenDto.setToken(token);

        try {
            redisTemplate.opsForValue().set(redisKey, redisTokenDto, Duration.ofHours(2));
            log.info("✅ Token stored in Redis for user: {}", username);
            redisAvailable = true;
        } catch (DataAccessException e) {
            redisAvailable = false;
            log.error("❌ Unable to store token in Redis. Falling back to PostgreSQL");
            RedisBackupDto redisBackupDto = new RedisBackupDto();
            redisBackupDto.setUsername(username);
            redisBackupDto.setToken(token);
            redisBackupRepository.save(RedisBackupEntityMapper.toEntity(redisBackupDto));
        }
    }

    @Scheduled(fixedRate = 60000)
    public void checkRedisHealthAndRestoreData() {
        if (redisAvailable) {
            return;
        }

        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            redisAvailable = true;
            log.info("✅ Redis connection restored. Restoring tokens...");

            LocalDateTime threeHoursAgo = LocalDateTime.now().minusHours(3);
            List<RedisBackupEntity> backupTokens = redisBackupRepository.findByCreatedAtAfter(threeHoursAgo);

            for (RedisBackupEntity backupEntity : backupTokens) {
                saveToken(backupEntity.getUsername(), backupEntity.getToken());
            }

            log.info("✅ Successfully restored {} tokens to Redis.", backupTokens.size());
        } catch (DataAccessException ex) {
            redisAvailable = false;
            log.warn("⚠️❌ Redis is still unavailable.");
        }
    }
}
