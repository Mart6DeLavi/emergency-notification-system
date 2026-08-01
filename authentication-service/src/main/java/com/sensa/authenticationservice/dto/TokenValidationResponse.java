package com.sensa.authenticationservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenValidationResponse(
        boolean valid,
        UUID userId,
        String email
) {}
