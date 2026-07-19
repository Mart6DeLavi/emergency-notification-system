package com.sensa.templateservice.service;

import com.sensa.templateservice.dto.TemplateRequest;
import com.sensa.templateservice.dto.TemplateResponse;
import com.sensa.templateservice.exception.TemplateAlreadyExistException;
import com.sensa.templateservice.exception.TemplateNotFoundException;
import com.sensa.templateservice.mapper.TemplateMapper;
import com.sensa.templateservice.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {
    private final TemplateRepository templateRepository;
    private final TemplateMapper templateMapper;

    public TemplateResponse createTemplate(String username, TemplateRequest templateRequest) {
        return Optional.of(templateRepository.existTemplateByUsernameAndTitle(username, templateRequest.title()))
                .filter(exist -> !exist)
                .map(unused -> templateMapper.mapToEntity(templateRequest))
                .map(template -> template.addUsername(username))
                .map(templateRepository::save)
                .map(templateMapper::mapToResponse)
                .orElseThrow(() -> new TemplateAlreadyExistException(
                        String.format("Template with title %s already exists for user: %s", templateRequest.title(), username)
                ));
    }

    public TemplateResponse findTemplate(String username, String title) {
        return templateRepository.findByClientUsernameAndTitle(username, title)
                .map(templateMapper::mapToResponse)
                .orElseThrow(() -> new TemplateNotFoundException(
                   String.format("Template with title %s not found", title)
                ));
    }

    public TemplateResponse updateTemplate(
            String username, String title,
            TemplateRequest templateRequest) {
        return templateRepository.findByClientUsernameAndTitle(username, title)
                .map(existingTemplate -> {
                    Optional.ofNullable(templateRequest.title()).ifPresent(existingTemplate::setTitle);
                    Optional.ofNullable(templateRequest.content()).ifPresent(existingTemplate::setContent);
                    return existingTemplate;
                })
                .map(templateRepository::save)
                .map(templateMapper::mapToResponse)
                .orElseThrow(() -> new TemplateNotFoundException(
                        String.format("Template with title %s not found for user: %s", templateRequest.title(), username)
                ));
    }

    public Boolean deleteTemplate(String username, String title) {
        int rowsDeleted = templateRepository.deleteByClientUsernameAndTitle(username, title);
        return rowsDeleted != 0;
    }
}
