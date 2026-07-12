package com.sensa.messagedeliveryservice.dto.client.notification;

import com.sensa.messagedeliveryservice.model.NotificationStatus;
import com.sensa.messagedeliveryservice.model.PreferredChannel;
import lombok.Builder;

@Builder
public record NotificationRequest(
        String username,
        String senderEmail,
        String title,
        String content,
        NotificationStatus status,
        PreferredChannel preferredChannel) {
}
