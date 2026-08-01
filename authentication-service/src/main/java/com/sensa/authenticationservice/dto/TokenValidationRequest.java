package com.sensa.authenticationservice.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenValidationRequest(

        @NotBlank(message = "Token is required")
        String token
) {}
