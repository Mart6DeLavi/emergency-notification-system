package com.sensa.usermanagementservice.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserAuthenticationAnswerDto {
    private UserAuthenticationAnswer userAuthenticationAnswer;

    public UserAuthenticationAnswerDto(UserAuthenticationAnswer userAuthenticationAnswer) {
        this.userAuthenticationAnswer = userAuthenticationAnswer;
    }
}
