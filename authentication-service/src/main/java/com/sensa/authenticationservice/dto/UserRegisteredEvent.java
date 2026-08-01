package com.sensa.authenticationservice.dto;

import java.util.UUID;

public record UserRegisteredEvent(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String country,
        String city,
        String street,
        String homeNumber
) {}
