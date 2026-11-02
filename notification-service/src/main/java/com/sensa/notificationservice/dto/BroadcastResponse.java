package com.sensa.notificationservice.dto;

import lombok.Builder;

@Builder
public record BroadcastResponse(
        int recipients,
        int deliveries
) {}
