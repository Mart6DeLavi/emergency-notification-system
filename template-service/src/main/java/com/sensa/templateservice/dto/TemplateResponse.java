package com.sensa.templateservice.dto;

import lombok.Builder;

@Builder
public record TemplateResponse(
        String clientUsername,
        String title,
        String content
) {
}
