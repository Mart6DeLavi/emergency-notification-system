package com.sensa.notificationservice.dto;

import com.sensa.notificationservice.model.NotificationStatus;
import com.sensa.notificationservice.model.PreferredChannel;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
public record NotificationResponse(
        String clientUsername,
        String senderEmail,
        String title,
        String content,
        NotificationStatus status,
        PreferredChannel preferredChannel,
        LocalDateTime createdAt,
        String message) {
}
