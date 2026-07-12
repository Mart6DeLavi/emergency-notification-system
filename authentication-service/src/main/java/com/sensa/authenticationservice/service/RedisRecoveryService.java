package com.sensa.authenticationservice.service;

import com.sensa.authenticationservice.dto.RedisBackupDto;
import com.sensa.authenticationservice.entity.RedisBackupEntity;
import com.sensa.authenticationservice.mapper.RedisBackupEntityMapper;
import com.sensa.authenticationservice.repository.RedisBackupRepository;
import com.sensa.authenticationservice.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRecoveryService {

    private final RedisBackupRepository redisBackupRepository;
    private final RedisUtils redisUtils;

    private volatile boolean redisAvailable = true;

    public void saveToken(String username, String token) {
        if (redisUtils.isRedisAvailable()) {
            redisUtils.saveTokenToRedis(username, token);
            redisAvailable = true;
        } else {
            redisAvailable = false;
            log.error("❌ Redis unavailable, storing token in PostgreSQL");
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

        if (redisUtils.isRedisAvailable()) {
            redisAvailable = true;
            log.info("✅ Redis connection restored. Restoring tokens...");

            LocalDateTime threeHoursAgo = LocalDateTime.now().minusHours(3);
            List<RedisBackupEntity> backupTokens = redisBackupRepository.findByCreatedAtAfter(threeHoursAgo);

            for (RedisBackupEntity backupEntity : backupTokens) {
                saveToken(backupEntity.getUsername(), backupEntity.getToken());
            }

            log.info("✅ Successfully restored {} tokens to Redis.", backupTokens.size());
        } else {
            log.warn("⚠️ Redis is still unavailable.");
        }
    }
}
