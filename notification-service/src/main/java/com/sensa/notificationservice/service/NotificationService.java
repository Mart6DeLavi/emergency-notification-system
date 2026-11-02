package com.sensa.notificationservice.service;

import com.sensa.notificationservice.client.TemplateServiceClient;
import com.sensa.notificationservice.client.UserDataServiceClient;
import com.sensa.notificationservice.config.RabbitMQConfig;
import com.sensa.notificationservice.dto.BroadcastRequest;
import com.sensa.notificationservice.dto.BroadcastResponse;
import com.sensa.notificationservice.dto.NotificationRequest;
import com.sensa.notificationservice.dto.NotificationResponse;
import com.sensa.notificationservice.dto.kafka.EmergencyConfirmedEvent;
import com.sensa.notificationservice.dto.kafka.NotificationDeliveryEvent;
import com.sensa.notificationservice.dto.kafka.UserLocationResponse;
import com.sensa.notificationservice.entity.Notification;
import com.sensa.notificationservice.exception.NotificationNotFoundException;
import com.sensa.notificationservice.mapper.NotificationMapper;
import com.sensa.notificationservice.model.NotificationChannel;
import com.sensa.notificationservice.model.NotificationStatus;
import com.sensa.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final RabbitTemplate rabbitTemplate;
    private final TemplateServiceClient templateServiceClient;
    private final UserDataServiceClient userDataServiceClient;

    @CacheEvict(cacheNames = "notifications", key = "#userId")
    public NotificationResponse createNotification(NotificationRequest request, UUID userId) {
        log.info("Creating notification: templateName={}, userId={}", request.templateName(), userId);

        Notification entity = notificationMapper.toEntity(request, userId);
        entity = notificationRepository.save(entity);

        return notificationMapper.toResponse(entity);
    }

    @CacheEvict(cacheNames = "notifications", allEntries = true)
    public BroadcastResponse broadcast(BroadcastRequest request) {
        log.info("Operator broadcast: title={}, city={}, street={}",
                request.title(), request.city(), request.street());

        boolean areaTargeted = request.city() != null && !request.city().isBlank();
        List<UserLocationResponse> recipients = areaTargeted
                ? userDataServiceClient.getRecipientsByLocation(request.city(), request.street())
                : userDataServiceClient.getAllRecipients();

        String deliveryContent = request.message().replace("\n", "<br>");
        int alerted = 0;
        int deliveries = 0;

        for (UserLocationResponse recipient : recipients) {
            boolean push = recipient.push();
            boolean email = recipient.emailEnabled();
            boolean sms = recipient.sms();

            if (!push && !email && !sms) {
                continue;
            }

            NotificationChannel primary = push
                    ? NotificationChannel.PUSH
                    : email ? NotificationChannel.EMAIL : NotificationChannel.SMS;

            Notification entity = Notification.builder()
                    .userId(recipient.userId())
                    .templateName("operator-broadcast")
                    .title(request.title())
                    .content(request.message())
                    .channel(primary)
                    .status(NotificationStatus.PENDING)
                    .build();
            notificationRepository.save(entity);
            alerted++;

            if (push) {
                sendDelivery(recipient, "PUSH", request.title(), deliveryContent);
                deliveries++;
            }
            if (email) {
                sendDelivery(recipient, "EMAIL", request.title(), deliveryContent);
                deliveries++;
            }
            if (sms) {
                sendDelivery(recipient, "SMS", request.title(), deliveryContent);
                deliveries++;
            }
        }

        log.info("Operator broadcast finished: recipients={}, deliveries={}", alerted, deliveries);
        return new BroadcastResponse(alerted, deliveries);
    }

    @Cacheable(cacheNames = "notifications", key = "#userId")
    public List<NotificationResponse> getMyNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    public NotificationResponse getNotification(String id, UUID userId) {
        return notificationRepository.findByIdAndUserId(id, userId)
                .map(notificationMapper::toResponse)
                .orElseThrow(() -> new NotificationNotFoundException(
                        String.format("Notification with id %s not found", id)));
    }

    @CacheEvict(cacheNames = "notifications", key = "#userId")
    public void deleteNotification(String id, UUID userId) {
        long deleted = notificationRepository.deleteByIdAndUserId(id, userId);
        if (deleted == 0L) {
            throw new NotificationNotFoundException(
                    String.format("Notification with id %s not found", id));
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
        rabbitTemplate.convertAndSend(RabbitMQConfig.DELIVERY_QUEUE, event);
        log.info("Sent {} delivery for userId={} to queue {}", channel, recipient.userId(), RabbitMQConfig.DELIVERY_QUEUE);
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
