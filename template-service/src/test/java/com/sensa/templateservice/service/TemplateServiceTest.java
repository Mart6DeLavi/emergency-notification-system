package com.sensa.templateservice.service;

import com.sensa.templateservice.dto.TemplateRequest;
import com.sensa.templateservice.dto.TemplateResponse;
import com.sensa.templateservice.entity.Template;
import com.sensa.templateservice.exception.TemplateAlreadyExistException;
import com.sensa.templateservice.exception.TemplateNotFoundException;
import com.sensa.templateservice.mapper.TemplateMapper;
import com.sensa.templateservice.repository.TemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateServiceTest {

    @Mock
    private TemplateRepository templateRepository;

    @Mock
    private TemplateMapper templateMapper;

    @InjectMocks
    private TemplateService templateService;

    @Test
    void testCreateTemplateSuccess() {
        String username = "user1";
        TemplateRequest request = TemplateRequest.builder()
                .clientUsername(username)
                .title("Template1")
                .content("Content1")
                .build();

        // Сценарий: шаблон с таким заголовком не существует
        when(templateRepository.existTemplateByUsernameAndTitle(username, request.title())).thenReturn(false);

        // Маппинг DTO в сущность
        Template templateEntity = Template.builder()
                .clientUsername(null)
                .title(request.title())
                .content(request.content())
                .build();
        when(templateMapper.mapToEntity(request)).thenReturn(templateEntity);

        // Предположим, что метод addUsername устанавливает username в сущность и возвращает её
        Template updatedTemplate = templateEntity.addUsername(username);

        // При сохранении возвращается обновлённая сущность
        when(templateRepository.save(updatedTemplate)).thenReturn(updatedTemplate);

        // Маппер преобразует сущность в ответ
        TemplateResponse response = TemplateResponse.builder()
                .clientUsername(username)
                .title(request.title())
                .content(request.content())
                .build();
        when(templateMapper.mapToResponse(updatedTemplate)).thenReturn(response);

        TemplateResponse result = templateService.createTemplate(username, request);
        assertEquals(response, result);
    }

    @Test
    void testCreateTemplateAlreadyExists() {
        String username = "user1";
        TemplateRequest request = TemplateRequest.builder()
                .clientUsername(username)
                .title("Template1")
                .content("Content1")
                .build();

        // Сценарий: шаблон уже существует
        when(templateRepository.existTemplateByUsernameAndTitle(username, request.title())).thenReturn(true);

        TemplateAlreadyExistException exception = assertThrows(TemplateAlreadyExistException.class,
                () -> templateService.createTemplate(username, request));
        String expectedMessage = String.format("Template with title %s already exists for user: %s", request.title(), username);
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void testFindTemplateSuccess() {
        String username = "user1";
        String title = "Template1";
        Template templateEntity = Template.builder()
                .clientUsername(username)
                .title(title)
                .content("Content1")
                .build();

        when(templateRepository.findByClientUsernameAndTitle(username, title)).thenReturn(Optional.of(templateEntity));

        TemplateResponse response = TemplateResponse.builder()
                .clientUsername(username)
                .title(title)
                .content("Content1")
                .build();
        when(templateMapper.mapToResponse(templateEntity)).thenReturn(response);

        TemplateResponse result = templateService.findTemplate(username, title);
        assertEquals(response, result);
    }

    @Test
    void testFindTemplateNotFound() {
        String username = "user1";
        String title = "Template1";
        when(templateRepository.findByClientUsernameAndTitle(username, title)).thenReturn(Optional.empty());

        TemplateNotFoundException exception = assertThrows(TemplateNotFoundException.class,
                () -> templateService.findTemplate(username, title));
        String expectedMessage = String.format("Template with title %s not found", title);
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void testUpdateTemplateSuccess() {
        String username = "user1";
        String oldTitle = "Template1";
        TemplateRequest request = TemplateRequest.builder()
                .clientUsername(username)
                .title("UpdatedTemplate")
                .content("UpdatedContent")
                .build();

        Template existingTemplate = Template.builder()
                .clientUsername(username)
                .title(oldTitle)
                .content("OldContent")
                .build();

        when(templateRepository.findByClientUsernameAndTitle(username, oldTitle)).thenReturn(Optional.of(existingTemplate));

        // Обновляем поля: в сервисе вызывается Optional.ofNullable(...).ifPresent(...)
        existingTemplate.setTitle(request.title());
        existingTemplate.setContent(request.content());

        when(templateRepository.save(existingTemplate)).thenReturn(existingTemplate);

        TemplateResponse response = TemplateResponse.builder()
                .clientUsername(username)
                .title(request.title())
                .content(request.content())
                .build();
        when(templateMapper.mapToResponse(existingTemplate)).thenReturn(response);

        TemplateResponse result = templateService.updateTemplate(username, oldTitle, request);
        assertEquals(response, result);
    }

    @Test
    void testUpdateTemplateNotFound() {
        String username = "user1";
        String oldTitle = "Template1";
        TemplateRequest request = TemplateRequest.builder()
                .clientUsername(username)
                .title("NewTitle")
                .content("NewContent")
                .build();

        when(templateRepository.findByClientUsernameAndTitle(username, oldTitle)).thenReturn(Optional.empty());

        TemplateNotFoundException exception = assertThrows(TemplateNotFoundException.class,
                () -> templateService.updateTemplate(username, oldTitle, request));
        String expectedMessage = String.format("Template with title %s not found for user: %s", request.title(), username);
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void testDeleteTemplateSuccess() {
        String username = "user1";
        String title = "Template1";
        when(templateRepository.deleteByClientUsernameAndTitle(username, title)).thenReturn(1);

        Boolean result = templateService.deleteTemplate(username, title);
        assertTrue(result);
    }

    @Test
    void testDeleteTemplateFailure() {
        String username = "user1";
        String title = "Template1";
        when(templateRepository.deleteByClientUsernameAndTitle(username, title)).thenReturn(0);

        Boolean result = templateService.deleteTemplate(username, title);
        assertFalse(result);
    }
}
