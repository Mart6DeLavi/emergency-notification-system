package com.sensa.notificationservice.service;

import com.sensa.notificationservice.client.TemplateClient;
import com.sensa.notificationservice.client.UserManagementClient;
import com.sensa.notificationservice.dto.NotificationRequest;
import com.sensa.notificationservice.dto.NotificationResponse;
import com.sensa.notificationservice.dto.client.ClientResponse;
import com.sensa.notificationservice.dto.kafka.NotificationKafkaDelivery;
import com.sensa.notificationservice.dto.template.TemplateResponse;
import com.sensa.notificationservice.entity.Notification;
import com.sensa.notificationservice.exception.InvalidNotificationStatusTransitionException;
import com.sensa.notificationservice.exception.NotificationNotFoundException;
import com.sensa.notificationservice.mapper.NotificationMapper;
import com.sensa.notificationservice.mapper.TemplateMapper;
import com.sensa.notificationservice.model.NotificationStatus;
import com.sensa.notificationservice.model.PreferredCommunicationChannel;
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
    private final UserManagementClient userManagementClient;
    private final NotificationMapper mapper;
    private final KafkaProducerService kafkaProducerService;
    private final NotificationMapper notificationMapper;

    public NotificationResponse createNotification(NotificationRequest request) {
      boolean existsTemplate = templateClient.findTemplate(request.username(), request.title())
              .getStatusCode().is2xxSuccessful();

      if (existsTemplate) {
          ClientResponse user = userManagementClient.getClientByUsername(request.username()).getBody();
          if (user != null && user.preferredCommunicationChannel().equals(PreferredCommunicationChannel.EMAIL)) {
              kafkaProducerService.sendEmailTopic(request);
          } else if (user != null && user.preferredCommunicationChannel().equals(PreferredCommunicationChannel.SMS)) {
              kafkaProducerService.sendSmsTopic(request);
          }
      } else {
          templateClient.createTemplate(request.username(), TemplateMapper.mapToRequest(request));
          ClientResponse user = userManagementClient.getClientByUsername(request.username()).getBody();
          if (user != null && user.preferredCommunicationChannel().equals(PreferredCommunicationChannel.EMAIL)) {
              kafkaProducerService.sendEmailTopic(request);
          } else if (user != null && user.preferredCommunicationChannel().equals(PreferredCommunicationChannel.SMS)) {
              kafkaProducerService.sendSmsTopic(request);
          }
      }
      NotificationResponse response = notificationMapper.mapToResponse(request);
      return response;
    }


    public NotificationResponse setStatus(NotificationRequest request, NotificationStatus status) {
        return NotificationResponse.builder()
                .clientUsername(request.username())
                .senderEmail(request.senderEmail())
                .title(request.title())
                .content(request.content())
                .status(status)
                .preferredChannel(request.preferredChannel())
                .build();
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
