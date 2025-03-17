package com.sensa.templateservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record TemplateRequest(
        @NotBlank
        String clientUsername,

        @NotBlank
        String title,

        @NotBlank
        String content) {
}
