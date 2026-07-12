package com.sensa.authenticationservice.mapper;

import com.sensa.authenticationservice.dto.UserAuthenticationDto;
import com.sensa.authenticationservice.entity.AuthEntity;

public class UserAuthenticationDtoMapper {
    public static AuthEntity toEntity(UserAuthenticationDto dto) {
        AuthEntity entity = new AuthEntity();
        entity.setUsername(dto.username());
        entity.setPassword(dto.password());
        return entity;
    }
}
