package com.sensa.authenticationservice.service;

import com.sensa.authenticationservice.config.security.BasicSecurityConfig;
import com.sensa.authenticationservice.dto.*;
import com.sensa.authenticationservice.entity.AuthEntity;
import com.sensa.authenticationservice.kafka.UserManagementServiceKafkaProducer;
import com.sensa.authenticationservice.mapper.RedisBackupEntityMapper;
import com.sensa.authenticationservice.mapper.UserAuthenticationDtoMapper;
import com.sensa.authenticationservice.repository.RedisBackupRepository;
import com.sensa.authenticationservice.repository.UserStorageRepository;
import com.sensa.authenticationservice.util.JwtTokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserManagementServiceKafkaProducer userManagementServiceKafkaProducer;
    private final UserStorageRepository userStorageRepository;
    private final JwtTokenUtils jwtTokenUtils;
    private final RedisBackupRepository redisBackupRepository;
    private final BasicSecurityConfig basicSecurityConfig;
    private final RedisTemplate<String, RedisTokenDto> redisTemplate;

    private final Queue<CompletableFuture<UserAuthenticationAnswerDto>> pendingRequests = new ConcurrentLinkedDeque<>();

    public String saveUserData(UserAuthenticationDto userAuthenticationDto) {
        AuthEntity entity = UserAuthenticationDtoMapper.toEntity(userAuthenticationDto);
        userStorageRepository.save(entity);
        userManagementServiceKafkaProducer.sendToUserManagementService(userAuthenticationDto);
        return "User data saved successfully";
    }

    public String generateToken(UserAuthenticationDto userAuthenticationDto) {
        UUID requestId = UUID.randomUUID();

        CompletableFuture<UserAuthenticationAnswerDto> future = new CompletableFuture<>();
        pendingRequests.add(future);
        userManagementServiceKafkaProducer.sendToUserManagementService(new UserAuthenticationDto(
                userAuthenticationDto.username(),
                basicSecurityConfig.passwordEncoder().encode(userAuthenticationDto.password())
        ));

        try {
            UserAuthenticationAnswerDto response = future.get(5, TimeUnit.SECONDS);
            if (response.getAnswer().equals(UserAuthenticationAnswer.FOUND)) {
                String token = jwtTokenUtils.generateToken(userAuthenticationDto);
                log.info("✅ Generated token for user: {}", userAuthenticationDto.username());

                RedisBackupDto redisBackupDto = new RedisBackupDto();
                redisBackupDto.setUsername(userAuthenticationDto.username());
                redisBackupDto.setToken(token);
                redisBackupRepository.save(RedisBackupEntityMapper.toEntity(redisBackupDto));

                String redisKey = "user: " + userAuthenticationDto.username();
                RedisTokenDto redisTokenDto = new RedisTokenDto();
                redisTokenDto.setUsername(userAuthenticationDto.username());
                redisTokenDto.setToken(token);
                redisTemplate.opsForValue().set(redisKey, redisTokenDto, Duration.ofHours(2));
                log.info("Token stored in Redis for user: {}", userAuthenticationDto.username());

                return token;
            } else {
                log.warn("❌ User not found in User Management Service.");
                return null;
            }
        } catch (TimeoutException ex) {
            log.error("⏳ Timeout: No response from User Management Service");
            return null;
        } catch (Exception ex) {
            log.error("❌ Unexpected error while waiting for response", ex);
            return null;
        } finally {
            pendingRequests.remove(requestId);
        }
    }

    @KafkaListener(topics = "user-management-to-authentication-service",
            groupId = "authentication-group",
            containerFactory = "userAuthenticationKafkaListenerContainerFactory"
    )
    public void processKafkaResponse(UserAuthenticationAnswerDto response) {
        log.info("Received kafka response: {}", response);
        CompletableFuture<UserAuthenticationAnswerDto> future = pendingRequests.poll();
        if (future != null) {
            future.complete(response);
        } else {
            log.warn("⚠️ Received response from unknown request: {}", response);
        }
    }
}
