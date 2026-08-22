package com.sensa.notificationservice.service;

import com.sensa.notificationservice.dto.NotificationRequest;
import com.sensa.notificationservice.dto.NotificationResponse;
import com.sensa.notificationservice.dto.kafka.NotificationDeliveryEvent;
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
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
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
    private RestTemplate restTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private final UUID userId = UUID.randomUUID();
    private final String jwtToken = "test-jwt";

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

        NotificationDeliveryEvent event = NotificationDeliveryEvent.builder()
                .userId(userId)
                .templateName("alert-template")
                .title("Emergency Alert")
                .content("Evacuation required")
                .channel("push")
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
        when(notificationMapper.toDeliveryEvent(entity)).thenReturn(event);
        when(notificationMapper.toResponse(entity)).thenReturn(response);

        NotificationResponse result = notificationService.createNotification(request, userId, jwtToken);

        assertNotNull(result);
        assertEquals("Emergency Alert", result.title());
        assertEquals(NotificationChannel.PUSH, result.channel());
        verify(kafkaTemplate).send(eq("notification.push"), eq(event));
    }

    @Test
    void createNotification_EmailChannel() {
        NotificationRequest request = NotificationRequest.builder()
                .templateName("welcome")
                .title("Welcome")
                .content("Hello!")
                .channel(NotificationChannel.EMAIL)
                .build();

        Notification entity = Notification.builder()
                .id(2L)
                .userId(userId)
                .templateName("welcome")
                .title("Welcome")
                .content("Hello!")
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.PENDING)
                .build();

        NotificationDeliveryEvent event = NotificationDeliveryEvent.builder()
                .userId(userId)
                .templateName("welcome")
                .title("Welcome")
                .content("Hello!")
                .channel("email")
                .build();

        when(notificationMapper.toEntity(request, userId)).thenReturn(entity);
        when(notificationRepository.save(entity)).thenReturn(entity);
        when(notificationMapper.toDeliveryEvent(entity)).thenReturn(event);
        when(notificationMapper.toResponse(entity)).thenReturn(mock(NotificationResponse.class));

        notificationService.createNotification(request, userId, jwtToken);

        verify(kafkaTemplate).send(eq("notification.email"), eq(event));
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
}
