package com.sensa.templateservice.dto;

import lombok.Builder;

@Builder
public record TemplateRequest(
        String clientUsername,
        String title,
        String content) {
}
