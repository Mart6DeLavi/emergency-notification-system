package com.sensa.notificationservice.dto;

import lombok.Builder;

@Builder
public record TemplateDto(
        Long id,
        String templateName,
        String description,
        String channel,
        String content,
        String createdBy
) {}
