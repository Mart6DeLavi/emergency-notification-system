package com.sensa.notificationservice.mapper;

import com.sensa.notificationservice.dto.NotificationRequest;
import com.sensa.notificationservice.dto.NotificationResponse;
import com.sensa.notificationservice.dto.kafka.NotificationDeliveryEvent;
import com.sensa.notificationservice.dto.kafka.UserLocationResponse;
import com.sensa.notificationservice.entity.Notification;
import com.sensa.notificationservice.model.NotificationStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class NotificationMapper {

    public Notification toEntity(NotificationRequest request, UUID userId) {
        return Notification.builder()
                .userId(userId)
                .templateName(request.templateName())
                .title(request.title())
                .content(request.content())
                .channel(request.channel())
                .status(NotificationStatus.PENDING)
                .build();
    }

    public NotificationResponse toResponse(Notification entity) {
        return NotificationResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .templateName(entity.getTemplateName())
                .title(entity.getTitle())
                .content(entity.getContent())
                .channel(entity.getChannel())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public NotificationDeliveryEvent toDeliveryEvent(UserLocationResponse recipient, String channel, String title, String content) {
        return NotificationDeliveryEvent.builder()
                .userId(recipient.userId())
                .email(recipient.email())
                .phoneNumber(recipient.phoneNumber())
                .channel(channel)
                .title(title)
                .content(content)
                .build();
    }
}
