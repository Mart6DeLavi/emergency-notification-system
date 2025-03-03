package com.sensa.notificationservice.service;

import com.sensa.notificationservice.dto.NotificationRequest;
import com.sensa.notificationservice.dto.kafka.NotificationKafkaDelivery;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {
    private final KafkaTemplate<String, NotificationKafkaDelivery> kafkaTemplate;

    @Value("${spring.kafka.topics.notifications.email}")
    private String emailTopic;

    @Value("${spring.kafka.topics.notifications.sms}")
    private String smsTopic;

    public void sendEmailTopic(NotificationRequest notificationRequest) {
        NotificationKafkaDelivery message = NotificationKafkaDelivery.builder()
                .clientUsername(notificationRequest.username())
                .producerEmail(notificationRequest.senderEmail())
                .title(notificationRequest.title())
                .content(notificationRequest.content())
                .build();

        kafkaTemplate.send(emailTopic, message);
    }

    public void sendSmsTopic(NotificationRequest notificationRequest) {
        NotificationKafkaDelivery message = NotificationKafkaDelivery.builder()
                .clientUsername(notificationRequest.username())
                .producerEmail(notificationRequest.senderEmail())
                .title(notificationRequest.title())
                .content(notificationRequest.content())
                .build();

        kafkaTemplate.send(smsTopic, message);
    }
}

