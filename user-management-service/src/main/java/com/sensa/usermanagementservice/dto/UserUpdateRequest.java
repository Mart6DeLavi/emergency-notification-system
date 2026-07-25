package com.sensa.usermanagementservice.dto;

public record UserUpdateRequest(
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    String country,
    String city,
    String street,
    String homeNumber,
    NotificationSettingsDto notificationSettings
) {}
