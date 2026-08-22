package com.sensa.notificationservice.mapper;

import com.sensa.notificationservice.dto.NotificationRequest;
import com.sensa.notificationservice.dto.NotificationResponse;
import com.sensa.notificationservice.dto.kafka.NotificationDeliveryEvent;
import com.sensa.notificationservice.entity.Notification;
import com.sensa.notificationservice.model.NotificationChannel;
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

    public NotificationDeliveryEvent toDeliveryEvent(Notification entity) {
        return NotificationDeliveryEvent.builder()
                .userId(entity.getUserId())
                .templateName(entity.getTemplateName())
                .title(entity.getTitle())
                .content(entity.getContent())
                .channel(entity.getChannel().name().toLowerCase())
                .build();
    }
}
