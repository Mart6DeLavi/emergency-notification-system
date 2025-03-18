package com.sensa.authenticationservice.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sensa.authenticationservice.dto.UserAuthenticationDto;
import org.apache.kafka.common.serialization.Serializer;
import org.hibernate.type.SerializationException;

public class UserAuthenticationDtoSerializer implements Serializer<UserAuthenticationDto> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] serialize(String topic, UserAuthenticationDto data) {
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (Exception ex) {
            throw new SerializationException("Error serializing user authentication dto", ex);
        }
    }
}
