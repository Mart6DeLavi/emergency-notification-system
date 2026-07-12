package com.sensa.authenticationservice.config.security;

import com.sensa.authenticationservice.util.JwtTokenUtils;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestFilter extends OncePerRequestFilter {

    private final JwtTokenUtils jwtTokenUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("Incoming request: {} {}", request.getMethod(), request.getRequestURL());

        String authHeader = request.getHeader("Authorization");
        String username = null;
        String jwtToken = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            jwtToken = authHeader.substring(7);
            log.info("Extracted JWT token: {}", jwtToken);

            try {
                if (jwtTokenUtils.isValidToken(jwtToken)) {
                    username = jwtTokenUtils.getUsernameFromToken(jwtToken);
                    log.info("Valid JWT token for user: {}", username);
                } else {
                    log.warn("Invalid JWT token received: ");
                }
            } catch (ExpiredJwtException ex) {
                log.error("Expired JWT token: {}", ex.getMessage());
            } catch (SignatureException ex) {
                log.error("Invalid signature in JWT token: {}", ex.getMessage());
            } catch (Exception ex) {
                log.error("Exception while validating JWT token: {}", ex.getMessage());
            }
        } else {
            log.warn("Authorization header is missing or does not start with 'Bearer '");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(token);
            log.info("Authentication set for user: {}", username);
        } else {
            log.warn("No valid username found, skipping authentication");
        }
        filterChain.doFilter(request, response);
    }
}
