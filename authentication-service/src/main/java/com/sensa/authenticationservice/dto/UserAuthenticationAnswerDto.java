package com.sensa.authenticationservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserAuthenticationAnswerDto {

    @JsonProperty("userAuthenticationAnswer")
    private UserAuthenticationAnswer answer;
}
