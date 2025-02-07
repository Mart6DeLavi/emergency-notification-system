package com.sensa.authenticationservice.mapper;

import com.sensa.authenticationservice.dto.UserAuthenticationDto;
import com.sensa.authenticationservice.entity.AuthEntity;

public class AuthEntityMapper {

    public static UserAuthenticationDto toDto(final AuthEntity authEntity) {
        return new UserAuthenticationDto(
                authEntity.getUsername(),
                authEntity.getPassword()
        );
    }
}
