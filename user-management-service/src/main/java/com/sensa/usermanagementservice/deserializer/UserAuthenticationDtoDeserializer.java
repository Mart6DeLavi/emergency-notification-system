package com.sensa.usermanagementservice.deserializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sensa.usermanagementservice.dto.UserAuthenticationDto;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class UserAuthenticationDtoDeserializer implements Deserializer<UserAuthenticationDto> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Deserializer.super.configure(configs, isKey);
    }

    @Override
    public UserAuthenticationDto deserialize(String topic, byte[] data) {
        try {
            return objectMapper.readValue(data, UserAuthenticationDto.class);
        } catch (Exception ex) {
            throw new RuntimeException("Error deserializing UserAuthenticationDto", ex);
        }
    }

    @Override
    public void close() {
        Deserializer.super.close();
    }
}
