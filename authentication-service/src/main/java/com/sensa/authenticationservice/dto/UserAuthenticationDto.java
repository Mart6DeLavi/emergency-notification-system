package com.sensa.authenticationservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UserAuthenticationDto(

        @NotBlank(message = "Username can not be empty")
        String username,

        @NotBlank(message = "Password can not be empty")
        String password
) {
}
