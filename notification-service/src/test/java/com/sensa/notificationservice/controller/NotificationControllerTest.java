package com.sensa.notificationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sensa.notificationservice.dto.NotificationRequest;
import com.sensa.notificationservice.dto.NotificationResponse;
import com.sensa.notificationservice.model.NotificationChannel;
import com.sensa.notificationservice.model.NotificationStatus;
import com.sensa.notificationservice.service.NotificationService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationService notificationService;

    private final UUID userId = UUID.randomUUID();
    private String jwtToken;

    @BeforeEach
    void setUp() {
        SecretKey key = Keys.hmacShaKeyFor("test-secret-key-for-notification-service-testing-256-bits-long"
                .getBytes(StandardCharsets.UTF_8));
        jwtToken = Jwts.builder()
                .subject("test@example.com")
                .claim("userId", userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }

    @Test
    void createNotification_ShouldReturn201() throws Exception {
        NotificationRequest request = NotificationRequest.builder()
                .templateName("alert")
                .title("Alert")
                .content("Test content")
                .channel(NotificationChannel.PUSH)
                .build();

        NotificationResponse response = NotificationResponse.builder()
                .id(1L)
                .userId(userId)
                .templateName("alert")
                .title("Alert")
                .content("Test content")
                .channel(NotificationChannel.PUSH)
                .status(NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationService.createNotification(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/notifications")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Alert"));
    }

    @Test
    void createNotification_InvalidBody_ShouldReturn400() throws Exception {
        NotificationRequest request = NotificationRequest.builder()
                .templateName("")
                .title("")
                .content("")
                .channel(null)
                .build();

        mockMvc.perform(post("/api/v1/notifications")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMyNotifications_ShouldReturn200() throws Exception {
        NotificationResponse response = NotificationResponse.builder()
                .id(1L)
                .userId(userId)
                .templateName("alert")
                .title("Alert")
                .content("Content")
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.PENDING)
                .build();

        when(notificationService.getMyNotifications(any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Alert"));
    }

    @Test
    void getNotification_ShouldReturn200() throws Exception {
        NotificationResponse response = NotificationResponse.builder()
                .id(1L)
                .userId(userId)
                .templateName("alert")
                .title("Alert")
                .content("Content")
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.PENDING)
                .build();

        when(notificationService.getNotification(eq(1L), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/notifications/1")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deleteNotification_ShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/v1/notifications/1")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isNoContent());
    }
}
