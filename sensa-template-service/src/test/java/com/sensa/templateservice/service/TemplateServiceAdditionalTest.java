package com.sensa.templateservice.service;

import com.sensa.templateservice.dto.TemplateRequest;
import com.sensa.templateservice.dto.TemplateResponse;
import com.sensa.templateservice.entity.Template;
import com.sensa.templateservice.exception.TemplateAlreadyExistException;
import com.sensa.templateservice.mapper.TemplateMapper;
import com.sensa.templateservice.repository.TemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TemplateServiceAdditionalTest {

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private TemplateMapper templateMapper;

    @InjectMocks
    private TemplateService templateService;

    @Test
    void testCreateTemplateMappingFailure() {
        String username = "user1";
        TemplateRequest request = TemplateRequest.builder()
                .clientUsername(username)
                .title("Template1")
                .content("Content1")
                .build();

        when(templateRepository.existTemplateByUsernameAndTitle(username, request.title())).thenReturn(false);
        when(templateMapper.mapToEntity(request)).thenReturn(null);

        TemplateAlreadyExistException exception = assertThrows(
                TemplateAlreadyExistException.class,
                () -> templateService.createTemplate(username, request)
        );
        String expectedMessage = String.format("Template with title %s already exists for user: %s", request.title(), username);
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void testUpdateTemplatePartialUpdateNoChanges() {
        String username = "user1";
        String title = "Template1";
        TemplateRequest request = TemplateRequest.builder()
                .clientUsername(username)
                .title(null)
                .content(null)
                .build();

        Template existingTemplate = Template.builder()
                .clientUsername(username)
                .title(title)
                .content("InitialContent")
                .build();

        when(templateRepository.findByClientUsernameAndTitle(username, title)).thenReturn(Optional.of(existingTemplate));
        when(templateRepository.save(existingTemplate)).thenReturn(existingTemplate);

        TemplateResponse expectedResponse = TemplateResponse.builder()
                .clientUsername(username)
                .title(title)
                .content("InitialContent")
                .build();
        when(templateMapper.mapToResponse(existingTemplate)).thenReturn(expectedResponse);

        TemplateResponse result = templateService.updateTemplate(username, title, request);
        assertEquals(expectedResponse, result);
    }

    @Test
    void testUpdateTemplateSaveThrowsException() {
        String username = "user1";
        String title = "Template1";
        TemplateRequest request = TemplateRequest.builder()
                .clientUsername(username)
                .title("NewTitle")
                .content("NewContent")
                .build();

        Template existingTemplate = Template.builder()
                .clientUsername(username)
                .title(title)
                .content("OldContent")
                .build();

        when(templateRepository.findByClientUsernameAndTitle(username, title)).thenReturn(Optional.of(existingTemplate));
        when(templateRepository.save(existingTemplate)).thenThrow(new RuntimeException("DB error"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> templateService.updateTemplate(username, title, request));
        assertEquals("DB error", exception.getMessage());
    }

    @Test
    void testDeleteTemplateNegativeRows() {
        String username = "user1";
        String title = "Template1";
        when(templateRepository.deleteByClientUsernameAndTitle(username, title)).thenReturn(-1);
        Boolean result = templateService.deleteTemplate(username, title);
        assertTrue(result);
    }
}
