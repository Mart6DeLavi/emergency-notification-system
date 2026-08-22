package com.sensa.notificationservice.dto.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.UUID;

@Builder
public record UserLocationResponse(
        @JsonProperty("userId") UUID userId,
        @JsonProperty("email") String email,
        @JsonProperty("phoneNumber") String phoneNumber,
        @JsonProperty("firstName") String firstName,
        @JsonProperty("lastName") String lastName,
        @JsonProperty("push") boolean push,
        @JsonProperty("emailEnabled") boolean emailEnabled,
        @JsonProperty("sms") boolean sms
) {}
