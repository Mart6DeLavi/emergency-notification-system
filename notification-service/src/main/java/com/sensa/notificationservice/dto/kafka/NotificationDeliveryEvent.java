package com.sensa.notificationservice.dto.kafka;

import lombok.Builder;

import java.util.UUID;

@Builder
public record NotificationDeliveryEvent(
        UUID userId,
        String email,
        String phoneNumber,
        String channel,
        String title,
        String content
) {}
