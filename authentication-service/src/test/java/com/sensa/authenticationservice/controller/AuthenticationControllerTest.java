package com.sensa.authenticationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sensa.authenticationservice.dto.AuthResponse;
import com.sensa.authenticationservice.dto.LoginRequest;
import com.sensa.authenticationservice.dto.RegisterRequest;
import com.sensa.authenticationservice.dto.TokenValidationRequest;
import com.sensa.authenticationservice.dto.TokenValidationResponse;
import com.sensa.authenticationservice.service.AuthService;
import com.sensa.authenticationservice.util.JwtTokenUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testRegisterSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        RegisterRequest request = new RegisterRequest(
                "test@example.com", "password123",
                "John", "Doe", "+1234567890",
                "USA", "NY", "Broadway", "10"
        );
        AuthResponse response = new AuthResponse("jwt-token", userId, "test@example.com");

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    void testRegisterMissingEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "", "password123",
                "John", "Doe", "+1234567890",
                "USA", "NY", "Broadway", "10"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLoginSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        AuthResponse response = new AuthResponse("jwt-token", userId, "test@example.com");

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void testLoginBadCredentials() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "wrong");
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Invalid email or password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testValidateSuccess() throws Exception {
        TokenValidationRequest request = new TokenValidationRequest("valid-jwt");
        UUID userId = UUID.randomUUID();
        TokenValidationResponse response = new TokenValidationResponse(true, userId, "test@example.com");

        when(authService.validate("valid-jwt")).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }
}
