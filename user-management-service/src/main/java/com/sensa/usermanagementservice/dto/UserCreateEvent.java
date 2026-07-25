package com.sensa.usermanagementservice.dto;

import java.util.UUID;

public record UserCreateEvent(
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
