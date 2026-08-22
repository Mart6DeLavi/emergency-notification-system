package com.sensa.notificationservice.dto.kafka;

import lombok.Builder;

import java.util.UUID;

@Builder
public record NotificationDeliveryEvent(
        UUID userId,
        String templateName,
        String title,
        String content,
        String channel
) {}
