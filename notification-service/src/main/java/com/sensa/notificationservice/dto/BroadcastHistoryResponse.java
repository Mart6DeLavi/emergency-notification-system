package com.sensa.notificationservice.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record BroadcastHistoryResponse(
        String id,
        String alertType,
        String title,
        String message,
        String audience,
        String city,
        String street,
        Double radiusKm,
        int recipients,
        int deliveries,
        int filtered,
        int errors,
        List<Long> deliveryTimeline,
        String status,
        LocalDateTime createdAt
) {}
