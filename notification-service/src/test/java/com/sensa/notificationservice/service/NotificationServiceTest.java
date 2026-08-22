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
import com.sensa.notificationservice.model.NotificationChannel;
import com.sensa.notificationservice.model.NotificationStatus;
import com.sensa.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private KafkaTemplate<String, NotificationDeliveryEvent> kafkaTemplate;

    @Mock
    private TemplateServiceClient templateServiceClient;

    @Mock
    private UserDataServiceClient userDataServiceClient;

    @InjectMocks
    private NotificationService notificationService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void createNotification_Success() {
        NotificationRequest request = NotificationRequest.builder()
                .templateName("alert-template")
                .title("Emergency Alert")
                .content("Evacuation required")
                .channel(NotificationChannel.PUSH)
                .build();

        Notification entity = Notification.builder()
                .id(1L)
                .userId(userId)
                .templateName("alert-template")
                .title("Emergency Alert")
                .content("Evacuation required")
                .channel(NotificationChannel.PUSH)
                .status(NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        NotificationResponse response = NotificationResponse.builder()
                .id(1L)
                .userId(userId)
                .templateName("alert-template")
                .title("Emergency Alert")
                .content("Evacuation required")
                .channel(NotificationChannel.PUSH)
                .status(NotificationStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(notificationMapper.toEntity(request, userId)).thenReturn(entity);
        when(notificationRepository.save(entity)).thenReturn(entity);
        when(notificationMapper.toResponse(entity)).thenReturn(response);

        NotificationResponse result = notificationService.createNotification(request, userId);

        assertNotNull(result);
        assertEquals("Emergency Alert", result.title());
        assertEquals(NotificationChannel.PUSH, result.channel());
        verify(notificationRepository).save(entity);
        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    @Test
    void getMyNotifications_ReturnsList() {
        Notification entity = Notification.builder()
                .id(1L)
                .userId(userId)
                .templateName("test")
                .title("Test")
                .content("Test content")
                .channel(NotificationChannel.PUSH)
                .status(NotificationStatus.PENDING)
                .build();

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(entity));
        when(notificationMapper.toResponse(entity)).thenReturn(mock(NotificationResponse.class));

        List<NotificationResponse> result = notificationService.getMyNotifications(userId);

        assertFalse(result.isEmpty());
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void getNotification_Success() {
        Notification entity = Notification.builder()
                .id(1L)
                .userId(userId)
                .title("Test")
                .build();

        when(notificationRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(entity));
        when(notificationMapper.toResponse(entity)).thenReturn(mock(NotificationResponse.class));

        NotificationResponse result = notificationService.getNotification(1L, userId);

        assertNotNull(result);
    }

    @Test
    void getNotification_NotFound() {
        when(notificationRepository.findByIdAndUserId(99L, userId)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class,
                () -> notificationService.getNotification(99L, userId));
    }

    @Test
    void deleteNotification_Success() {
        when(notificationRepository.deleteByIdAndUserId(1L, userId)).thenReturn(1);

        assertDoesNotThrow(() -> notificationService.deleteNotification(1L, userId));
    }

    @Test
    void deleteNotification_NotFound() {
        when(notificationRepository.deleteByIdAndUserId(99L, userId)).thenReturn(0);

        assertThrows(NotificationNotFoundException.class,
                () -> notificationService.deleteNotification(99L, userId));
    }

    @Test
    void handleEmergencyConfirmed_RendersAndBroadcasts() {
        ReflectionTestUtils.setField(notificationService, "deliveryTopic", "notification.delivery");

        EmergencyConfirmedEvent event = EmergencyConfirmedEvent.builder()
                .emergencyId(100L)
                .title("Fire alert")
                .description("Big fire")
                .city("Warsaw")
                .street("Main St")
                .templateName("emergency-alert")
                .templateData(Map.of("city", "Warsaw", "street", "Main St"))
                .build();

        when(templateServiceClient.getTemplateContent("emergency-alert"))
                .thenReturn("Alert in {{city}} on {{street}}");

        UserLocationResponse recipient = UserLocationResponse.builder()
                .userId(userId)
                .email("user@example.com")
                .phoneNumber("123456789")
                .firstName("John")
                .lastName("Doe")
                .push(true)
                .emailEnabled(true)
                .sms(false)
                .build();

        when(userDataServiceClient.getRecipientsByLocation("Warsaw", "Main St"))
                .thenReturn(List.of(recipient));

        NotificationDeliveryEvent pushEvent = NotificationDeliveryEvent.builder()
                .userId(userId)
                .email("user@example.com")
                .phoneNumber("123456789")
                .channel("PUSH")
                .title("Fire alert")
                .content("Alert in Warsaw on Main St")
                .build();
        NotificationDeliveryEvent emailEvent = NotificationDeliveryEvent.builder()
                .userId(userId)
                .email("user@example.com")
                .phoneNumber("123456789")
                .channel("EMAIL")
                .title("Fire alert")
                .content("Alert in Warsaw on Main St")
                .build();

        when(notificationMapper.toDeliveryEvent(eq(recipient), eq("PUSH"), eq("Fire alert"), eq("Alert in Warsaw on Main St")))
                .thenReturn(pushEvent);
        when(notificationMapper.toDeliveryEvent(eq(recipient), eq("EMAIL"), eq("Fire alert"), eq("Alert in Warsaw on Main St")))
                .thenReturn(emailEvent);

        notificationService.handleEmergencyConfirmed(event);

        verify(kafkaTemplate).send("notification.delivery", pushEvent);
        verify(kafkaTemplate).send("notification.delivery", emailEvent);
    }

    @Test
    void handleEmergencyConfirmed_NoRecipients_NoDelivery() {
        ReflectionTestUtils.setField(notificationService, "deliveryTopic", "notification.delivery");

        EmergencyConfirmedEvent event = EmergencyConfirmedEvent.builder()
                .emergencyId(101L)
                .title("Fire alert")
                .city("Warsaw")
                .street("Main St")
                .templateName("emergency-alert")
                .templateData(Map.of())
                .build();

        when(templateServiceClient.getTemplateContent("emergency-alert"))
                .thenReturn("Alert");
        when(userDataServiceClient.getRecipientsByLocation("Warsaw", "Main St"))
                .thenReturn(List.of());

        notificationService.handleEmergencyConfirmed(event);

        verify(kafkaTemplate, never()).send(anyString(), any());
    }
}
