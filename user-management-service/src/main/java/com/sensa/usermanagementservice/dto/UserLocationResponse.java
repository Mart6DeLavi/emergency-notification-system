package com.sensa.usermanagementservice.dto;

import java.util.UUID;

public record UserLocationResponse(
        UUID userId,
        String email,
        String phoneNumber,
        String firstName,
        String lastName,
        String deviceToken,
        boolean push,
        boolean emailEnabled,
        boolean sms
) {}
