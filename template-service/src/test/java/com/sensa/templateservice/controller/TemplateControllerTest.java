package com.sensa.templateservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sensa.templateservice.dto.TemplateRequest;
import com.sensa.templateservice.dto.TemplateResponse;
import com.sensa.templateservice.service.TemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(TemplateController.class)
class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TemplateService templateService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateTemplate() throws Exception {
        String username = "user1";
        TemplateRequest request = TemplateRequest.builder()
                .clientUsername(username)
                .title("Template1")
                .content("Content1")
                .build();

        TemplateResponse response = TemplateResponse.builder()
                .clientUsername(username)
                .title("Template1")
                .content("Content1")
                .build();

        when(templateService.createTemplate(username, request)).thenReturn(response);

        mockMvc.perform(post("/api/v1/templates/create")
                        .header("username", username)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientUsername").value(username))
                .andExpect(jsonPath("$.title").value("Template1"))
                .andExpect(jsonPath("$.content").value("Content1"));
    }

    @Test
    void testFindTemplate() throws Exception {
        String username = "user1";
        String title = "Template1";
        TemplateResponse response = TemplateResponse.builder()
                .clientUsername(username)
                .title(title)
                .content("Content1")
                .build();

        when(templateService.findTemplate(username, title)).thenReturn(response);

        mockMvc.perform(get("/api/v1/templates/find/{title}", title)
                        .header("username", username))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientUsername").value(username))
                .andExpect(jsonPath("$.title").value(title));
    }

    @Test
    void testUpdateTemplate() throws Exception {
        String username = "user1";
        String title = "Template1";
        TemplateRequest request = TemplateRequest.builder()
                .clientUsername(username)
                .title("UpdatedTemplate")
                .content("UpdatedContent")
                .build();

        TemplateResponse response = TemplateResponse.builder()
                .clientUsername(username)
                .title("UpdatedTemplate")
                .content("UpdatedContent")
                .build();

        when(templateService.updateTemplate(username, title, request)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/templates/update/{title}", title)
                        .header("username", username)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientUsername").value(username))
                .andExpect(jsonPath("$.title").value("UpdatedTemplate"))
                .andExpect(jsonPath("$.content").value("UpdatedContent"));
    }

    @Test
    void testDeleteTemplate() throws Exception {
        String username = "user1";
        String title = "Template1";

        when(templateService.deleteTemplate(username, title)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/templates/delete/{title}", title)
                        .header("username", username))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}