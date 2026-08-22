package com.sensa.notificationservice.service;

import com.sensa.notificationservice.dto.NotificationRequest;
import com.sensa.notificationservice.dto.NotificationResponse;
import com.sensa.notificationservice.dto.TemplateDto;
import com.sensa.notificationservice.dto.kafka.NotificationDeliveryEvent;
import com.sensa.notificationservice.entity.Notification;
import com.sensa.notificationservice.exception.NotificationNotFoundException;
import com.sensa.notificationservice.mapper.NotificationMapper;
import com.sensa.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final KafkaTemplate<String, NotificationDeliveryEvent> kafkaTemplate;
    private final RestTemplate restTemplate;

    @Value("${services.template-service.url}")
    private String templateServiceUrl;

    @Transactional
    public NotificationResponse createNotification(NotificationRequest request, UUID userId, String jwtToken) {
        log.info("Creating notification: templateName={}, userId={}", request.templateName(), userId);

        String topicName = "notification." + request.channel().name().toLowerCase();

        Notification entity = notificationMapper.toEntity(request, userId);
        entity = notificationRepository.save(entity);

        NotificationDeliveryEvent event = notificationMapper.toDeliveryEvent(entity);
        kafkaTemplate.send(topicName, event);
        log.info("Notification sent to Kafka topic: {}", topicName);

        return notificationMapper.toResponse(entity);
    }

    public List<NotificationResponse> getMyNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    public NotificationResponse getNotification(Long id, UUID userId) {
        return notificationRepository.findByIdAndUserId(id, userId)
                .map(notificationMapper::toResponse)
                .orElseThrow(() -> new NotificationNotFoundException(
                        String.format("Notification with id %d not found", id)));
    }

    @Transactional
    public void deleteNotification(Long id, UUID userId) {
        int deleted = notificationRepository.deleteByIdAndUserId(id, userId);
        if (deleted == 0) {
            throw new NotificationNotFoundException(
                    String.format("Notification with id %d not found", id));
        }
    }
}
