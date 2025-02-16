package com.sensa.usermanagementservice.dto;

import com.sensa.usermanagementservice.data.enums.PreferredCommunicationChannel;
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
