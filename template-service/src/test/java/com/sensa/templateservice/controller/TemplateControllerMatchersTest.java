package com.sensa.templateservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sensa.templateservice.dto.TemplateRequest;
import com.sensa.templateservice.dto.TemplateResponse;
import com.sensa.templateservice.entity.TemplateChannel;
import com.sensa.templateservice.service.TemplateService;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TemplateControllerMatchersTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TemplateService templateService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UUID userId = UUID.randomUUID();
    private final LocalDateTime now = LocalDateTime.now();
    private String jwtToken;

    @BeforeEach
    void setUp() {
        SecretKey key = Keys.hmacShaKeyFor(
                "test-secret-key-for-template-service-testing-256bit".getBytes(StandardCharsets.UTF_8));
        jwtToken = Jwts.builder()
                .subject("test@example.com")
                .claim("userId", userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }

    private TemplateResponse makeResponse(String templateName, TemplateChannel channel) {
        return new TemplateResponse(1L, templateName, "Test description", channel, "Test content",
                userId, now, now);
    }

    @Test
    void createTemplate_ShouldReturn201() throws Exception {
        var request = new TemplateRequest(
                "alert-template", "Emergency alert", TemplateChannel.PUSH, "Alert: {{message}}");

        when(templateService.create(any(), any())).thenReturn(makeResponse("alert-template", TemplateChannel.PUSH));

        mockMvc.perform(
                        post("/api/v1/templates")
                                .header("Authorization", "Bearer " + jwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.templateName").value("alert-template"));
    }

    @Test
    void createTemplate_Duplicate_ShouldReturn409() throws Exception {
        var request = new TemplateRequest(
                "existing", null, TemplateChannel.EMAIL, "Content");

        when(templateService.create(any(), any()))
                .thenThrow(new com.sensa.templateservice.exception.TemplateAlreadyExistsException("existing"));

        mockMvc.perform(
                        post("/api/v1/templates")
                                .header("Authorization", "Bearer " + jwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void updateTemplate_ShouldReturn200() throws Exception {
        var request = new TemplateRequest(
                "updated", null, TemplateChannel.EMAIL, "Updated content");

        when(templateService.update(any(), any()))
                .thenReturn(makeResponse("updated", TemplateChannel.EMAIL));

        mockMvc.perform(
                        patch("/api/v1/templates/alert-template")
                                .header("Authorization", "Bearer " + jwtToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.templateName").value("updated"));
    }
}
