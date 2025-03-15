package com.sensa.usermanagementservice.dto;

import lombok.Builder;
import lombok.Setter;

@Setter
@Builder
public record UserAuthenticationDto(
        String username,
        String password
) {
}
