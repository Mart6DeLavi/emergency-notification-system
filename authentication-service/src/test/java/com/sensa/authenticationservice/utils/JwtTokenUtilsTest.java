package com.sensa.authenticationservice.utils;

import com.sensa.authenticationservice.dto.UserAuthenticationDto;
import com.sensa.authenticationservice.util.JwtTokenUtils;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenUtilsTest {

    private JwtTokenUtils jwtTokenUtils;
    private final String secret = "mySecretKey";
    private final Duration lifetime = Duration.ofMinutes(30);

    @BeforeEach
    void setUp() {
        jwtTokenUtils = new JwtTokenUtils();
        ReflectionTestUtils.setField(jwtTokenUtils, "userSecret", secret);
        ReflectionTestUtils.setField(jwtTokenUtils, "userSecretLifetime", lifetime);
    }

    @Test
    void testGenerateTokenAndParse() {
        UserAuthenticationDto dto = new UserAuthenticationDto("testUser", "testPassword");
        String token = jwtTokenUtils.generateToken(dto);
        assertNotNull(token);
        assertTrue(jwtTokenUtils.isValidToken(token));

        Claims claims = jwtTokenUtils.getAllClaimsFromToken(token);
        assertEquals("testUser", claims.getSubject());
        assertEquals("testUser", claims.get("username"));
    }

    @Test
    void testInvalidToken() {
        String invalidToken = "invalid.token.string";
        assertFalse(jwtTokenUtils.isValidToken(invalidToken));
    }

    @Test
    void testGetUsernameFromToken() {
        UserAuthenticationDto dto = new UserAuthenticationDto("anotherUser", "pass");
        String token = jwtTokenUtils.generateToken(dto);
        String username = jwtTokenUtils.getUsernameFromToken(token);
        assertEquals("anotherUser", username);
    }
}
