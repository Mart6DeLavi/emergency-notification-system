package com.sensa.authenticationservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
public class JwtTokenUtils {

    private final SecretKey secretKey;
    private final long lifetimeMs;

    public JwtTokenUtils(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.lifetime}") String lifetime) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.lifetimeMs = parseLifetime(lifetime);
    }

    private long parseLifetime(String lifetime) {
        String num = lifetime.replaceAll("[^0-9]", "");
        if (lifetime.endsWith("ms")) return Long.parseLong(num);
        if (lifetime.endsWith("s")) return Long.parseLong(num) * 1000L;
        if (lifetime.endsWith("m")) return Long.parseLong(num) * 60 * 1000L;
        if (lifetime.endsWith("h")) return Long.parseLong(num) * 60 * 60 * 1000L;
        if (lifetime.endsWith("d")) return Long.parseLong(num) * 24 * 60 * 60 * 1000L;
        return 24 * 60 * 60 * 1000L;
    }

    public String generateToken(UUID userId, String email) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + lifetimeMs);

        return Jwts.builder()
                .claim("userId", userId.toString())
                .claim("email", email)
                .subject(email)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValidToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
            return false;
        }
    }

    public UUID getUserIdFromToken(String token) {
        String userId = parseToken(token).get("userId", String.class);
        return UUID.fromString(userId);
    }

    public String getEmailFromToken(String token) {
        return parseToken(token).getSubject();
    }
}
