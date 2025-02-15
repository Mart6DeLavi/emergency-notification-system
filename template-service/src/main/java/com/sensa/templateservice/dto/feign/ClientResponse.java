package com.sensa.templateservice.dto.feign;

public record ClientResponse(
        Long id,
        String username,
        String email,
        String phoneNumber,
        PreferredCommunicationChannel preferredCommunicationChannel
) {
}
