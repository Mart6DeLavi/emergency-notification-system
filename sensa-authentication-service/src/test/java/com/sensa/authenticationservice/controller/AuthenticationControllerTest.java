package com.sensa.authenticationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sensa.authenticationservice.dto.UserAuthenticationDto;
import com.sensa.authenticationservice.service.AuthenticationService;
import com.sensa.authenticationservice.util.JwtTokenUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationService authenticationService;

    @MockBean
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testAuthenticationSuccess() throws Exception {
        UserAuthenticationDto authDto = new UserAuthenticationDto("testUser", "testPassword");
        when(authenticationService.saveUserData(authDto)).thenReturn("User data saved successfully");
        when(authenticationService.generateToken(authDto)).thenReturn("dummyToken");

        mockMvc.perform(post("/api/v1/authentication/new")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authDto)))
                .andExpect(status().isOk())
                .andExpect(content().string("dummyToken"));
    }
}