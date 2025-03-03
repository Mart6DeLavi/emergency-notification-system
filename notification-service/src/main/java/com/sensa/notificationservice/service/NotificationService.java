package com.sensa.notificationservice.service;

import com.sensa.notificationservice.client.TemplateClient;
import com.sensa.notificationservice.dto.NotificationRequest;
import com.sensa.notificationservice.dto.NotificationResponse;
import com.sensa.notificationservice.dto.kafka.NotificationKafkaDelivery;
import com.sensa.notificationservice.entity.Notification;
import com.sensa.notificationservice.exception.InvalidNotificationStatusTransitionException;
import com.sensa.notificationservice.exception.NotificationNotFoundException;
import com.sensa.notificationservice.mapper.NotificationMapper;
import com.sensa.notificationservice.model.NotificationStatus;
import com.sensa.notificationservice.repository.NotificationRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final KafkaTemplate<String, NotificationKafkaDelivery> kafkaTemplate;
    private final NotificationRepository notificationRepository;
    private final TemplateClient templateClient;
    private final NotificationMapper mapper;
    private final KafkaProducerService kafkaProducerService;

    public NotificationResponse createNotification(NotificationRequest request) {
        try {
            boolean templateExists = templateClient.findTemplate(request.username(), request.title())
                    .getStatusCode().is2xxSuccessful();

            // Если шаблон существует в TemplateService
            if (templateExists) {
                return notificationRepository.findByClientUsernameAndTitle(request.username(), request.title())
                        .map(existingNotification -> {
                            // Если сообщение уже есть в локальной базе — повторная отправка
                            sendNotificationToKafka(existingNotification);
                            return mapper.mapToResponse(existingNotification)
                                    .toBuilder()
                                    .message("Template exists. Resending existing notification.")
                                    .build();
                        })
                        .orElseGet(() -> {
                            // Если сообщения с таким title в локальной базе нет — создаем новое
                            Notification newNotification = mapper.mapToEntity(request);
                            newNotification.setStatus(NotificationStatus.NEW);
                            Notification savedNotification = notificationRepository.save(newNotification);

                            // Отправляем через Kafka
                            sendNotificationToKafka(savedNotification);

                            return mapper.mapToResponse(savedNotification)
                                    .toBuilder()
                                    .message("Template exists. New message saved and sent.")
                                    .build();
                        });
            } else {
                // Если шаблона нет в TemplateService — возвращаем ошибку
                throw new RuntimeException("Template not found in Template Service.");
            }
        } catch (FeignException.NotFound e) {
            // Обработка случая, когда шаблон не найден в TemplateService
            throw new RuntimeException("Template not found in Template Service.");
        } catch (FeignException e) {
            // Обработка других ошибок от TemplateClient
            throw new RuntimeException("Error while communicating with Template Service: " + e.getMessage(), e);
        }
    }




    /**
     * Универсальный метод для изменения статуса уведомления
     */
    public NotificationResponse updateNotificationStatus(String clientUsername, NotificationStatus newStatus) {
        return notificationRepository.findByClientUsername(clientUsername)
                .map(notification -> {
                    // Проверяем возможность перехода в новый статус
                    if (!canTransitTo(notification.getStatus(), newStatus)) {
                        throw new InvalidNotificationStatusTransitionException(
                                String.format("Cannot transition from %s to %s",
                                        notification.getStatus(), newStatus)
                        );
                    }

                    // Обновляем статус и сохраняем изменения
                    notification.setStatus(newStatus);
                    Notification updatedNotification = notificationRepository.save(notification);

                    // Отправляем обновлённое уведомление в Kafka
                    sendNotificationToKafka(updatedNotification);

                    // Возвращаем ответ с обновленным статусом
                    return mapper.mapToResponse(updatedNotification)
                            .toBuilder()
                            .message(String.format("Status updated to %s.", newStatus))
                            .build();
                })
                .orElseThrow(() -> new NotificationNotFoundException(
                        String.format("Notification not found for clientUsername: %s", clientUsername)
                ));
    }


    /**
     * Отправка уведомления через Kafka
     */
    private void sendNotificationToKafka(Notification notification) {
        NotificationKafkaDelivery delivery = NotificationKafkaDelivery.builder()
                .clientUsername(notification.getClientUsername())
                .producerEmail(notification.getSenderEmail())
                .title(notification.getTitle())
                .content(notification.getContent())
                .build();

        kafkaTemplate.send(deliveryTopic, delivery);
    }

    private boolean canTransitTo(NotificationStatus currentStatus, NotificationStatus nextStatus) {
        return switch (currentStatus) {
            case NEW -> nextStatus == NotificationStatus.SENT || nextStatus == NotificationStatus.ERROR;
            case SENT -> nextStatus == NotificationStatus.RESENDING || nextStatus == NotificationStatus.ERROR;
            case RESENDING -> nextStatus == NotificationStatus.SENT || nextStatus == NotificationStatus.ERROR;
            case ERROR -> nextStatus == NotificationStatus.CORRUPT;
            case CORRUPT -> false;
        };
    }
}
