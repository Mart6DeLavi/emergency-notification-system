package com.sensa.authenticationservice.mapper;

import com.sensa.authenticationservice.dto.RedisBackupDto;
import com.sensa.authenticationservice.entity.RedisBackupEntity;

public class RedisBackupEntityMapper {
    public static RedisBackupEntity toEntity(final RedisBackupDto dto) {
        RedisBackupEntity entity = new RedisBackupEntity();
        entity.setUsername(dto.getUsername());
        entity.setToken(dto.getToken());
        return entity;
    }
}
