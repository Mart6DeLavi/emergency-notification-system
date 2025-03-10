package com.sensa.notificationservice.mapper;

import com.sensa.notificationservice.dto.NotificationRequest;
import com.sensa.notificationservice.dto.NotificationResponse;
import com.sensa.notificationservice.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {
    public Notification mapToEntity(NotificationRequest notificationRequest) {
        return Notification.builder()
                .clientUsername(notificationRequest.username())
                .senderEmail(notificationRequest.senderEmail())
                .title(notificationRequest.title())
                .content(notificationRequest.content())
                .preferredChannel(notificationRequest.preferredChannel())
                .build();
    }

    public NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .clientUsername(notification.getClientUsername())
                .senderEmail(notification.getSenderEmail())
                .title(notification.getTitle())
                .content(notification.getContent())
                .preferredChannel(notification.getPreferredChannel())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    public NotificationResponse mapToResponse(NotificationRequest notificationRequest) {
        return NotificationResponse.builder()
                .clientUsername(notificationRequest.username())
                .clientUsername(notificationRequest.senderEmail())
                .title(notificationRequest.title())
                .content(notificationRequest.content())
                .status(notificationRequest.status())
                .preferredChannel(notificationRequest.preferredChannel())
                .build();
    }

    public Notification update(NotificationRequest notificationRequest, Notification notification) {
        notification.setClientUsername(notificationRequest.username());
        notification.setTitle(notificationRequest.title());
        notification.setContent(notification.getContent());
        return notification;
    }
}
