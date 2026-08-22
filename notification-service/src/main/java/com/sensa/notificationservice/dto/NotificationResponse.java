package com.sensa.notificationservice.dto;

import com.sensa.notificationservice.model.NotificationChannel;
import com.sensa.notificationservice.model.NotificationStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record NotificationResponse(
        Long id,
        UUID userId,
        String templateName,
        String title,
        String content,
        NotificationChannel channel,
        NotificationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
