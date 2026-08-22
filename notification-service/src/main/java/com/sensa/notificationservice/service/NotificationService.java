package com.sensa.notificationservice.service;

import com.sensa.notificationservice.client.TemplateServiceClient;
import com.sensa.notificationservice.client.UserDataServiceClient;
import com.sensa.notificationservice.dto.NotificationRequest;
import com.sensa.notificationservice.dto.NotificationResponse;
import com.sensa.notificationservice.dto.kafka.EmergencyConfirmedEvent;
import com.sensa.notificationservice.dto.kafka.NotificationDeliveryEvent;
import com.sensa.notificationservice.dto.kafka.UserLocationResponse;
import com.sensa.notificationservice.entity.Notification;
import com.sensa.notificationservice.exception.NotificationNotFoundException;
import com.sensa.notificationservice.mapper.NotificationMapper;
import com.sensa.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final KafkaTemplate<String, NotificationDeliveryEvent> kafkaTemplate;
    private final TemplateServiceClient templateServiceClient;
    private final UserDataServiceClient userDataServiceClient;

    @Value("${spring.kafka.topics.delivery}")
    private String deliveryTopic;

    @Transactional
    public NotificationResponse createNotification(NotificationRequest request, UUID userId) {
        log.info("Creating notification: templateName={}, userId={}", request.templateName(), userId);

        Notification entity = notificationMapper.toEntity(request, userId);
        entity = notificationRepository.save(entity);

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

    public void handleEmergencyConfirmed(EmergencyConfirmedEvent event) {
        log.info("Handling emergency confirmed: emergencyId={}, template={}",
                event.emergencyId(), event.templateName());

        String templateContent = templateServiceClient.getTemplateContent(event.templateName());
        String renderedContent = render(templateContent, event.templateData());
        String title = event.title() != null ? event.title() : "";

        List<UserLocationResponse> recipients =
                userDataServiceClient.getRecipientsByLocation(event.city(), event.street());
        log.info("Broadcasting emergency {} to {} recipients", event.emergencyId(), recipients.size());

        for (UserLocationResponse recipient : recipients) {
            if (recipient.push()) {
                sendDelivery(recipient, "PUSH", title, renderedContent);
            }
            if (recipient.emailEnabled()) {
                sendDelivery(recipient, "EMAIL", title, renderedContent);
            }
            if (recipient.sms()) {
                sendDelivery(recipient, "SMS", title, renderedContent);
            }
        }
    }

    private void sendDelivery(UserLocationResponse recipient, String channel, String title, String content) {
        NotificationDeliveryEvent event = notificationMapper.toDeliveryEvent(recipient, channel, title, content);
        kafkaTemplate.send(deliveryTopic, event);
        log.info("Sent {} delivery for userId={} to topic {}", channel, recipient.userId(), deliveryTopic);
    }

    private String render(String template, Map<String, String> data) {
        if (template == null) {
            return "";
        }
        String result = template;
        if (data != null) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                if (entry.getValue() != null) {
                    result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
                }
            }
        }
        return result.replace("\n", "<br>");
    }
}
