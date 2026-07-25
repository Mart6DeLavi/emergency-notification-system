package com.sensa.usermanagementservice.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record UserResponse(
    UUID userId,
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    String country,
    String city,
    String street,
    String homeNumber,
    NotificationSettingsDto notificationSettings,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
