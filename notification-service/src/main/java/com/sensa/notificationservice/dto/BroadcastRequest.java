package com.sensa.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record BroadcastRequest(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Message is required")
        String message,

        String alertType,

        String audience,

        String city,

        String street,

        Double radiusKm
) {}
