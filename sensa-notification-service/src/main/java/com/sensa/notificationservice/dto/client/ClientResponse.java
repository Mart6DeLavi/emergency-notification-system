package com.sensa.notificationservice.dto.client;

import com.sensa.notificationservice.model.PreferredCommunicationChannel;
import lombok.Builder;

@Builder
public record ClientResponse(
        Long id,
        String username,
        String email,
        String phoneNumber,
        PreferredCommunicationChannel preferredCommunicationChannel
) {
}
