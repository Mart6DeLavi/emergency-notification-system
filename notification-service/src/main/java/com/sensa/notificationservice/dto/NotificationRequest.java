package com.sensa.notificationservice.dto;

import com.sensa.notificationservice.model.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record NotificationRequest(
        @NotBlank(message = "Template name is required")
        String templateName,

        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Content is required")
        String content,

        @NotNull(message = "Channel is required")
        NotificationChannel channel
) {}
