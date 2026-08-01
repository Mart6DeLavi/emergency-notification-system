package com.sensa.authenticationservice.service;

import com.sensa.authenticationservice.config.security.BasicSecurityConfig;
import com.sensa.authenticationservice.dto.AuthResponse;
import com.sensa.authenticationservice.dto.LoginRequest;
import com.sensa.authenticationservice.dto.RegisterRequest;
import com.sensa.authenticationservice.dto.TokenValidationResponse;
import com.sensa.authenticationservice.entity.AuthEntity;
import com.sensa.authenticationservice.kafka.UserRegisteredEventProducer;
import com.sensa.authenticationservice.repository.UserStorageRepository;
import com.sensa.authenticationservice.util.JwtTokenUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserStorageRepository userStorageRepository;

    @Mock
    private BasicSecurityConfig basicSecurityConfig;

    @Mock
    private JwtTokenUtils jwtTokenUtils;

    @Mock
    private UserRegisteredEventProducer userRegisteredEventProducer;

    @InjectMocks
    private AuthService authService;

    @Test
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest(
                "test@example.com", "password123",
                "John", "Doe", "+1234567890",
                "USA", "NY", "Broadway", "10"
        );

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        when(basicSecurityConfig.passwordEncoder()).thenReturn(encoder);
        when(userStorageRepository.existsByEmail(request.email())).thenReturn(false);
        when(jwtTokenUtils.generateToken(any(UUID.class), eq("test@example.com"))).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertNotNull(response.userId());
        assertEquals("test@example.com", response.email());
        assertEquals("jwt-token", response.token());

        ArgumentCaptor<AuthEntity> entityCaptor = ArgumentCaptor.forClass(AuthEntity.class);
        verify(userStorageRepository).save(entityCaptor.capture());
        AuthEntity savedEntity = entityCaptor.getValue();
        assertEquals("test@example.com", savedEntity.getEmail());
        assertNotNull(savedEntity.getUserId());
        assertNotEquals("password123", savedEntity.getPassword());

        verify(userRegisteredEventProducer).sendUserRegisteredEvent(any());
    }

    @Test
    void testRegisterEmailExists() {
        RegisterRequest request = new RegisterRequest(
                "test@example.com", "password123",
                "John", "Doe", "+1234567890",
                "USA", "NY", "Broadway", "10"
        );

        when(userStorageRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verify(userStorageRepository, never()).save(any());
    }

    @Test
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        UUID userId = UUID.randomUUID();
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = encoder.encode("password123");

        AuthEntity entity = new AuthEntity();
        entity.setId(1L);
        entity.setUserId(userId);
        entity.setEmail("test@example.com");
        entity.setPassword(hashedPassword);

        when(basicSecurityConfig.passwordEncoder()).thenReturn(encoder);
        when(userStorageRepository.findByEmail("test@example.com")).thenReturn(Optional.of(entity));
        when(jwtTokenUtils.generateToken(userId, "test@example.com")).thenReturn("jwt-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals(userId, response.userId());
        assertEquals("test@example.com", response.email());
        assertEquals("jwt-token", response.token());
    }

    @Test
    void testLoginEmailNotFound() {
        LoginRequest request = new LoginRequest("unknown@example.com", "password123");

        when(userStorageRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void testLoginWrongPassword() {
        LoginRequest request = new LoginRequest("test@example.com", "wrongpassword");

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPassword = encoder.encode("password123");

        AuthEntity entity = new AuthEntity();
        entity.setId(1L);
        entity.setUserId(UUID.randomUUID());
        entity.setEmail("test@example.com");
        entity.setPassword(hashedPassword);

        when(basicSecurityConfig.passwordEncoder()).thenReturn(encoder);
        when(userStorageRepository.findByEmail("test@example.com")).thenReturn(Optional.of(entity));

        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
    }

    @Test
    void testValidateTokenSuccess() {
        String token = "valid-jwt-token";
        UUID userId = UUID.randomUUID();

        when(jwtTokenUtils.parseToken(token)).thenReturn(
                io.jsonwebtoken.Jwts.claims().add("userId", userId.toString()).subject("test@example.com").build()
        );

        TokenValidationResponse response = authService.validate(token);

        assertTrue(response.valid());
        assertEquals(userId, response.userId());
        assertEquals("test@example.com", response.email());
    }

    @Test
    void testValidateTokenFailure() {
        String token = "invalid-jwt-token";

        when(jwtTokenUtils.parseToken(token)).thenThrow(new RuntimeException("Invalid token"));

        TokenValidationResponse response = authService.validate(token);

        assertFalse(response.valid());
        assertNull(response.userId());
        assertNull(response.email());
    }
}
