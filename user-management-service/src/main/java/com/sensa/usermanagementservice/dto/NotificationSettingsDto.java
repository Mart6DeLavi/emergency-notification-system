package com.sensa.usermanagementservice.dto;

public record NotificationSettingsDto(
    boolean push,
    boolean emailEnabled,
    boolean sms
) {}
