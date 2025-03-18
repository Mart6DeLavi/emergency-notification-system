package com.sensa.usermanagementservice.dto;

import com.sensa.usermanagementservice.model.PreferredCommunicationChannel;
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
