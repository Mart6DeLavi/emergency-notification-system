package com.sensa.authenticationservice.deserializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sensa.authenticationservice.dto.UserAuthenticationAnswerDto;
import org.apache.kafka.common.serialization.Deserializer;

public class UserAuthenticationAnswerDtoDeserializer implements Deserializer<UserAuthenticationAnswerDto> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public UserAuthenticationAnswerDto deserialize(String topic, byte[] data) {
        try {
            return objectMapper.readValue(data, UserAuthenticationAnswerDto.class);
        } catch (Exception ex) {
            throw new RuntimeException("Error deserializing UserAuthenticationAnswerDto", ex);
        }
    }
}
