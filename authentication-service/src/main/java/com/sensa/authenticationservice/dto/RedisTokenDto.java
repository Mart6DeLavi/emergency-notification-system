package com.sensa.authenticationservice.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.io.Serializable;


@Getter
@Setter
@RequiredArgsConstructor
public class RedisTokenDto implements Serializable {
    private String username;
    private String token;
}
