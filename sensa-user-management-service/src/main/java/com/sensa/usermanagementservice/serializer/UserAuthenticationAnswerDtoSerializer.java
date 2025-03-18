package com.sensa.usermanagementservice.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sensa.usermanagementservice.dto.UserAuthenticationAnswerDto;
import org.apache.kafka.common.serialization.Serializer;
import org.hibernate.type.SerializationException;

public class UserAuthenticationAnswerDtoSerializer implements Serializer<UserAuthenticationAnswerDto> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public byte[] serialize(String s, UserAuthenticationAnswerDto userAuthenticationAnswerDto) {
        try {
            return objectMapper.writeValueAsBytes(userAuthenticationAnswerDto);
        } catch (Exception ex) {
            throw new SerializationException("Error serializing user authentication dto", ex);
        }
    }
}
