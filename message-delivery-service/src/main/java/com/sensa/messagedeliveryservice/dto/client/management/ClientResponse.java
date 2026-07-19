package com.sensa.messagedeliveryservice.dto.client.management;

import com.sensa.messagedeliveryservice.model.PreferredCommunicationChannel;
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
