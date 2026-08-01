package com.sensa.authenticationservice.utils;

import com.sensa.authenticationservice.util.JwtTokenUtils;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenUtilsTest {

    private JwtTokenUtils jwtTokenUtils;

    @BeforeEach
    void setUp() {
        jwtTokenUtils = new JwtTokenUtils("my-secret-key-that-is-long-enough-for-hs256", "1h");
    }

    @Test
    void testGenerateTokenAndParse() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenUtils.generateToken(userId, "test@example.com");
        assertNotNull(token);
        assertTrue(jwtTokenUtils.isValidToken(token));

        Claims claims = jwtTokenUtils.parseToken(token);
        assertEquals("test@example.com", claims.getSubject());
        assertEquals(userId.toString(), claims.get("userId"));
        assertEquals("test@example.com", claims.get("email"));
    }

    @Test
    void testInvalidToken() {
        assertFalse(jwtTokenUtils.isValidToken("invalid.token.string"));
    }

    @Test
    void testGetUserIdFromToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenUtils.generateToken(userId, "user@example.com");
        UUID extractedId = jwtTokenUtils.getUserIdFromToken(token);
        assertEquals(userId, extractedId);
    }

    @Test
    void testGetEmailFromToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtTokenUtils.generateToken(userId, "user@example.com");
        String email = jwtTokenUtils.getEmailFromToken(token);
        assertEquals("user@example.com", email);
    }
}
