package com.sensa.notificationservice.dto.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.Map;

@Builder
public record EmergencyConfirmedEvent(
        @JsonProperty("emergency_id") Long emergencyId,
        @JsonProperty("title") String title,
        @JsonProperty("description") String description,
        @JsonProperty("city") String city,
        @JsonProperty("street") String street,
        @JsonProperty("country") String country,
        @JsonProperty("template_name") String templateName,
        @JsonProperty("template_data") Map<String, String> templateData
) {}
