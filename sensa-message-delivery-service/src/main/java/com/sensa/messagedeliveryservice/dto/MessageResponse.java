package com.sensa.messagedeliveryservice.dto;

import com.sensa.messagedeliveryservice.model.NotificationStatus;
import lombok.Builder;

@Builder
public record MessageResponse(
        String username,
        String email,
        String title,
        String content,
        NotificationStatus notificationStatus
) {
}
