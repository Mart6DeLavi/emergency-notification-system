package com.sensa.authenticationservice.service;

import com.sensa.authenticationservice.config.security.BasicSecurityConfig;
import com.sensa.authenticationservice.dto.UserAuthenticationAnswer;
import com.sensa.authenticationservice.dto.UserAuthenticationAnswerDto;
import com.sensa.authenticationservice.dto.UserAuthenticationDto;
import com.sensa.authenticationservice.entity.AuthEntity;
import com.sensa.authenticationservice.kafka.UserManagementServiceKafkaProducer;
import com.sensa.authenticationservice.mapper.UserAuthenticationDtoMapper;
import com.sensa.authenticationservice.repository.UserStorageRepository;
import com.sensa.authenticationservice.util.JwtTokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserManagementServiceKafkaProducer userManagementServiceKafkaProducer;
    private final UserStorageRepository userStorageRepository;
    private final JwtTokenUtils jwtTokenUtils;
    private final BasicSecurityConfig basicSecurityConfig;
    private final RedisRecoveryService redisRecoveryService;

    private final Queue<CompletableFuture<UserAuthenticationAnswerDto>> pendingRequests = new ConcurrentLinkedDeque<>();

    public String saveUserData(UserAuthenticationDto userAuthenticationDto) {
        return Stream.of(userAuthenticationDto)
                .map(UserAuthenticationDtoMapper::toEntity)
                .peek(userStorageRepository::save)
                .peek(entity -> userManagementServiceKafkaProducer.sendToUserManagementService(userAuthenticationDto))
                .map(entity -> "User data saved successfully")
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Failed to save user data"));
    }

    public String generateToken(UserAuthenticationDto userAuthenticationDto) {
        UUID requestId = UUID.randomUUID();
        CompletableFuture<UserAuthenticationAnswerDto> future = new CompletableFuture<>();

        pendingRequests.add(future);

        Stream.of(userAuthenticationDto)
                .map(dto -> new UserAuthenticationDto(dto.username(), basicSecurityConfig.passwordEncoder().encode(dto.password())))
                .forEach(userManagementServiceKafkaProducer::sendToUserManagementService);

        try {
            return Optional.ofNullable(future.get(5, TimeUnit.SECONDS))
                    .filter(response -> response.getAnswer().equals(UserAuthenticationAnswer.FOUND))
                    .map(response -> jwtTokenUtils.generateToken(userAuthenticationDto))
                    .map(token -> {
                        log.info("✅ Generated token for user: {}", userAuthenticationDto.username());
                        redisRecoveryService.saveToken(userAuthenticationDto.username(), token);
                        return token;
                    })
                    .orElseGet(() -> {
                        log.warn("❌ User not found in User Management Service.");
                        return null;
                    });
        } catch (TimeoutException ex) {
            log.error("⏳ Timeout: No response from User Management Service");
            return null;
        } catch (Exception ex) {
            log.error("❌ Unexpected error while waiting for response", ex);
            return null;
        } finally {
            pendingRequests.remove(future);
        }
    }

    @KafkaListener(topics = "user-management-to-authentication-service",
            groupId = "authentication-group",
            containerFactory = "userAuthenticationKafkaListenerContainerFactory"
    )
    public void processKafkaResponse(UserAuthenticationAnswerDto response) {
        log.info("✅ Received kafka response: {}", response);
        CompletableFuture<UserAuthenticationAnswerDto> future = pendingRequests.poll();
        if (future != null) {
            future.complete(response);
        } else {
            log.warn("⚠️ Received response from unknown request: {}", response.getAnswer());
        }
    }
}