package com.sensa.usermanagementservice.dto;

import com.sensa.usermanagementservice.model.PreferredCommunicationChannel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ClientDtoTest {
    @Test
    void testClientRegistrationDtoEquality() {
        ClientRegistrationDto dto1 = new ClientRegistrationDto(
                "John", "Doe", "john_doe", "john@example.com", "password",
                "+1234567890", 30, PreferredCommunicationChannel.EMAIL
        );
        ClientRegistrationDto dto2 = new ClientRegistrationDto(
                "John", "Doe", "john_doe", "john@example.com", "password",
                "+1234567890", 30, PreferredCommunicationChannel.EMAIL
        );
        assertEquals(dto1, dto2);
    }

    @Test
    void testClientResponseToString() {
        ClientResponse response = new ClientResponse(
                1L, "john_doe", "john@example.com", "+1234567890", PreferredCommunicationChannel.EMAIL
        );
        String str = response.toString();
        assertTrue(str.contains("john_doe"));
        assertTrue(str.contains("john@example.com"));
    }
}
