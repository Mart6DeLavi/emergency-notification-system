package com.sensa.messagedeliveryservice.dto.client.notification;

import com.sensa.messagedeliveryservice.model.NotificationStatus;
import com.sensa.messagedeliveryservice.model.PreferredChannel;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
public record NotificationResponse(
        String clientUsername,
        String senderEmail,
        String title,
        String content,
        NotificationStatus status,
        PreferredChannel preferredChannel,
        LocalDateTime createdAt,
        String message
) {

}
