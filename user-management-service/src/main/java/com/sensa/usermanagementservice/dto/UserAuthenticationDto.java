package com.sensa.usermanagementservice.dto;

import lombok.Builder;

@Builder
public record UserAuthenticationDto(
        String username,
        String password
) {
}
