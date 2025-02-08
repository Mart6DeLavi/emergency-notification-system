package com.sensa.authenticationservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class UserAuthenticationAnswerDto {

    @JsonProperty("userAuthenticationAnswer")
    private UserAuthenticationAnswer answer;
}
