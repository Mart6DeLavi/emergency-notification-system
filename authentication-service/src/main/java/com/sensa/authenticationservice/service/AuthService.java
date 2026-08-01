package com.sensa.authenticationservice.service;

import com.sensa.authenticationservice.config.security.BasicSecurityConfig;
import com.sensa.authenticationservice.dto.AuthResponse;
import com.sensa.authenticationservice.dto.LoginRequest;
import com.sensa.authenticationservice.dto.RegisterRequest;
import com.sensa.authenticationservice.dto.TokenValidationResponse;
import com.sensa.authenticationservice.dto.UserRegisteredEvent;
import com.sensa.authenticationservice.entity.AuthEntity;
import com.sensa.authenticationservice.kafka.UserRegisteredEventProducer;
import com.sensa.authenticationservice.repository.UserStorageRepository;
import com.sensa.authenticationservice.util.JwtTokenUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserStorageRepository userStorageRepository;
    private final BasicSecurityConfig basicSecurityConfig;
    private final JwtTokenUtils jwtTokenUtils;
    private final UserRegisteredEventProducer userRegisteredEventProducer;

    public AuthResponse register(RegisterRequest request) {
        if (userStorageRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        UUID userId = UUID.randomUUID();

        AuthEntity entity = new AuthEntity();
        entity.setUserId(userId);
        entity.setEmail(request.email());
        entity.setPassword(basicSecurityConfig.passwordEncoder().encode(request.password()));
        userStorageRepository.save(entity);

        UserRegisteredEvent event = new UserRegisteredEvent(
                userId,
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phoneNumber(),
                request.country(),
                request.city(),
                request.street(),
                request.homeNumber()
        );
        userRegisteredEventProducer.sendUserRegisteredEvent(event);

        String token = jwtTokenUtils.generateToken(userId, request.email());

        log.info("User registered: userId={}, email={}", userId, request.email());
        return new AuthResponse(token, userId, request.email());
    }

    public AuthResponse login(LoginRequest request) {
        AuthEntity entity = userStorageRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!basicSecurityConfig.passwordEncoder().matches(request.password(), entity.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtTokenUtils.generateToken(entity.getUserId(), entity.getEmail());

        log.info("User logged in: userId={}, email={}", entity.getUserId(), entity.getEmail());
        return new AuthResponse(token, entity.getUserId(), entity.getEmail());
    }

    public TokenValidationResponse validate(String token) {
        try {
            Claims claims = jwtTokenUtils.parseToken(token);
            UUID userId = UUID.fromString(claims.get("userId", String.class));
            String email = claims.getSubject();

            return new TokenValidationResponse(true, userId, email);
        } catch (Exception ex) {
            log.warn("Token validation failed: {}", ex.getMessage());
            return new TokenValidationResponse(false, null, null);
        }
    }
}
