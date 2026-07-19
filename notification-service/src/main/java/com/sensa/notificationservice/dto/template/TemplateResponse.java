package com.sensa.notificationservice.dto.template;

import lombok.Builder;

@Builder
public record TemplateResponse(
        String clientUsername,
        String title,
        String content) {
}
