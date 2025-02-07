package com.sensa.authenticationservice.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum UserAuthenticationAnswer {
    FOUND("Found user in database"),
    NOTEXIST("No such user id database");

    private final String message;

    UserAuthenticationAnswer(String message) {
        this.message = message;
    }

    @JsonValue
    public String getMessage() {
        return message;
    }

    @JsonCreator
    public static UserAuthenticationAnswer fromString(String value) {
        for (UserAuthenticationAnswer answer : UserAuthenticationAnswer.values()) {
            if (answer.name().equalsIgnoreCase(value)) {
                return answer;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }
}
