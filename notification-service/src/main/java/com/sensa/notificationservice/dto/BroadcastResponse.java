package com.sensa.notificationservice.dto;

import lombok.Builder;

@Builder
public record BroadcastResponse(
        String broadcastId,
        int recipients,
        int deliveries,
        int filtered,
        int errors
) {}
