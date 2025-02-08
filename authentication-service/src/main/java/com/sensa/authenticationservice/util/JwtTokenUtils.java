package com.sensa.authenticationservice.util;

import com.sensa.authenticationservice.dto.UserAuthenticationDto;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
@Component
public class JwtTokenUtils {

    @Value("${jwt.user_secret}")
    private String userSecret;

    @Value("${jwt.user_secret_lifetime}")
    private Duration userSecretLifetime;

    public String generateToken(UserAuthenticationDto dto) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", dto.username());

        Date issudeDate = new Date();
        Date expirationDate = new Date(issudeDate.getTime() + getUserSecretLifetime().toMillis());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(dto.username())
                .setIssuedAt(issudeDate)
                .setExpiration(expirationDate)
                .signWith(SignatureAlgorithm.HS256, getUserSecret())
                .compact();
    }


    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .setSigningKey(getUserSecret())
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isValidToken(String token) {
        try {
            getAllClaimsFromToken(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }
}
