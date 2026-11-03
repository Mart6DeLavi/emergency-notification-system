package com.sensa.notificationservice.service;

import com.sensa.notificationservice.client.TemplateServiceClient;
import com.sensa.notificationservice.client.UserDataServiceClient;
import com.sensa.notificationservice.config.RabbitMQConfig;
import com.sensa.notificationservice.dto.BroadcastHistoryResponse;
import com.sensa.notificationservice.dto.BroadcastRequest;
import com.sensa.notificationservice.dto.BroadcastResponse;
import com.sensa.notificationservice.dto.NotificationRequest;
import com.sensa.notificationservice.dto.NotificationResponse;
import com.sensa.notificationservice.dto.kafka.EmergencyConfirmedEvent;
import com.sensa.notificationservice.dto.kafka.NotificationDeliveryEvent;
import com.sensa.notificationservice.dto.kafka.UserLocationResponse;
import com.sensa.notificationservice.entity.Notification;
import com.sensa.notificationservice.entity.OperatorBroadcast;
import com.sensa.notificationservice.exception.NotificationNotFoundException;
import com.sensa.notificationservice.mapper.NotificationMapper;
import com.sensa.notificationservice.model.NotificationChannel;
import com.sensa.notificationservice.model.NotificationStatus;
import com.sensa.notificationservice.repository.NotificationRepository;
import com.sensa.notificationservice.repository.OperatorBroadcastRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final OperatorBroadcastRepository operatorBroadcastRepository;
    private final NotificationMapper notificationMapper;
    private final RabbitTemplate rabbitTemplate;
    private final TemplateServiceClient templateServiceClient;
    private final UserDataServiceClient userDataServiceClient;
    private final EmailTemplateRenderer emailTemplateRenderer;

    @CacheEvict(cacheNames = "notifications", key = "#userId")
    public NotificationResponse createNotification(NotificationRequest request, UUID userId) {
        log.info("Creating notification: templateName={}, userId={}", request.templateName(), userId);

        Notification entity = notificationMapper.toEntity(request, userId);
        entity = notificationRepository.save(entity);

        return notificationMapper.toResponse(entity);
    }

    @CacheEvict(cacheNames = "notifications", allEntries = true)
    public BroadcastResponse broadcast(BroadcastRequest request, UUID operatorId) {
        log.info("Operator broadcast: operatorId={}, title={}, city={}, street={}",
                operatorId, request.title(), request.city(), request.street());

        boolean areaTargeted = request.city() != null && !request.city().isBlank();
        List<UserLocationResponse> recipients = areaTargeted
                ? userDataServiceClient.getRecipientsByLocation(request.city(), request.street())
                : userDataServiceClient.getAllRecipients();

        String deliveryContent = request.message().replace("\n", "<br>");
        String emailContent = emailTemplateRenderer.render(
                request.alertType(),
                request.title(),
                request.message(),
                request.city(),
                request.street(),
                request.radiusKm());
        long startedAt = System.nanoTime();
        List<Long> deliveryTimeline = new ArrayList<>();
        int alerted = 0;
        int deliveries = 0;
        int filtered = 0;

        for (UserLocationResponse recipient : recipients) {
            boolean push = recipient.push();
            boolean email = recipient.emailEnabled();
            boolean sms = recipient.sms();

            if (!push && !email && !sms) {
                filtered++;
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
                deliveryTimeline.add(offsetMillis(startedAt));
            }
            if (email) {
                sendDelivery(recipient, "EMAIL", request.title(), emailContent);
                deliveries++;
                deliveryTimeline.add(offsetMillis(startedAt));
            }
            if (sms) {
                sendDelivery(recipient, "SMS", request.title(), deliveryContent);
                deliveries++;
                deliveryTimeline.add(offsetMillis(startedAt));
            }
        }

        OperatorBroadcast record = OperatorBroadcast.builder()
                .operatorId(operatorId != null ? operatorId.toString() : null)
                .alertType(normalize(request.alertType(), "OTHER"))
                .title(request.title())
                .message(request.message())
                .audience(normalize(request.audience(), areaTargeted ? "LOCATION" : "ALL"))
                .city(request.city())
                .street(request.street())
                .radiusKm(request.radiusKm())
                .recipients(alerted)
                .deliveries(deliveries)
                .filtered(filtered)
                .errors(0)
                .deliveryTimeline(deliveryTimeline)
                .status(alerted > 0 ? "DELIVERED" : "FILTERED")
                .build();
        record = operatorBroadcastRepository.save(record);

        log.info("Operator broadcast finished: recipients={}, deliveries={}, filtered={}",
                alerted, deliveries, filtered);
        return new BroadcastResponse(record.getId(), alerted, deliveries, filtered, 0);
    }

    public List<BroadcastHistoryResponse> getBroadcastHistory(UUID operatorId) {
        List<OperatorBroadcast> broadcasts = operatorId == null
                ? operatorBroadcastRepository.findTop20ByOrderByCreatedAtDesc()
                : operatorBroadcastRepository.findTop20ByOperatorIdOrderByCreatedAtDesc(operatorId.toString());

        return broadcasts.stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    private BroadcastHistoryResponse toHistoryResponse(OperatorBroadcast broadcast) {
        return BroadcastHistoryResponse.builder()
                .id(broadcast.getId())
                .alertType(broadcast.getAlertType())
                .title(broadcast.getTitle())
                .message(broadcast.getMessage())
                .audience(broadcast.getAudience())
                .city(broadcast.getCity())
                .street(broadcast.getStreet())
                .radiusKm(broadcast.getRadiusKm())
                .recipients(broadcast.getRecipients())
                .deliveries(broadcast.getDeliveries())
                .filtered(broadcast.getFiltered())
                .errors(broadcast.getErrors())
                .deliveryTimeline(broadcast.getDeliveryTimeline() != null
                        ? broadcast.getDeliveryTimeline()
                        : List.of())
                .status(broadcast.getStatus())
                .createdAt(broadcast.getCreatedAt())
                .build();
    }

    private long offsetMillis(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
    }

    private String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase();
    }

    @Cacheable(cacheNames = "notifications", key = "#userId")
    public List<NotificationResponse> getMyNotifications(UUID userId) {
        return new ArrayList<>(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(notificationMapper::toResponse)
                .toList());
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
        String title = event.title() != null && !event.title().isBlank()
                ? event.title()
                : "Emergency alert";
        String description = event.description() != null
                ? event.description()
                : event.templateData() == null
                        ? ""
                        : event.templateData().getOrDefault("description", "");
        String emailContent = emailTemplateRenderer.render(
                emailTemplateRenderer.detectType(title + " " + description),
                title,
                description,
                event.city(),
                event.street(),
                null);

        List<UserLocationResponse> recipients =
                userDataServiceClient.getRecipientsByLocation(event.city(), event.street());
        log.info("Broadcasting emergency {} to {} recipients", event.emergencyId(), recipients.size());

        for (UserLocationResponse recipient : recipients) {
            if (recipient.push()) {
                sendDelivery(recipient, "PUSH", title, renderedContent);
            }
            if (recipient.emailEnabled()) {
                sendDelivery(recipient, "EMAIL", title, emailContent);
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
