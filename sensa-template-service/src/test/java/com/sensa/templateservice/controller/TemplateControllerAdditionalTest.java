package com.sensa.templateservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sensa.templateservice.dto.TemplateRequest;
import com.sensa.templateservice.service.TemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(TemplateController.class)
class TemplateControllerAdditionalTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TemplateService templateService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateTemplateValidationError() throws Exception {
        String username = "user1";
        TemplateRequest request = TemplateRequest.builder()
                .clientUsername(username)
                .title("")
                .content("Content1")
                .build();

        mockMvc.perform(post("/api/v1/templates/create")
                        .header("username", username)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateTemplateMissingUsernameHeader() throws Exception {
        TemplateRequest request = TemplateRequest.builder()
                .clientUsername("user1")
                .title("Template1")
                .content("Content1")
                .build();

        mockMvc.perform(post("/api/v1/templates/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}