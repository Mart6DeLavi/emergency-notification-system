package com.sensa.notificationservice.controller;

import com.sensa.notificationservice.dto.client.ClientResponse;
import com.sensa.notificationservice.dto.NotificationRequest;
import com.sensa.notificationservice.dto.NotificationResponse;
import com.sensa.notificationservice.dto.template.TemplateResponse;
import com.sensa.notificationservice.model.NotificationStatus;
import com.sensa.notificationservice.model.PreferredChannel;
import com.sensa.notificationservice.model.PreferredCommunicationChannel;
import com.sensa.notificationservice.client.TemplateClient;
import com.sensa.notificationservice.client.UserManagementClient;
import com.sensa.notificationservice.mapper.NotificationMapper;
import com.sensa.notificationservice.repository.NotificationRepository;
import com.sensa.notificationservice.service.KafkaProducerService;
import com.sensa.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private TemplateClient templateClient;
    @Mock
    private UserManagementClient userManagementClient;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private KafkaProducerService kafkaProducerService;
    @InjectMocks
    private NotificationService notificationService;

    @Test
    void testCreateNotification_TemplateExists_Email() {
        NotificationRequest request = NotificationRequest.builder()
                .username("user1")
                .senderEmail("sender@example.com")
                .title("Test Title")
                .content("Test Content")
                .status(NotificationStatus.NEW)
                .preferredChannel(PreferredChannel.EMAIL)
                .build();

        TemplateResponse templateResponse = TemplateResponse.builder()
                .clientUsername("user1")
                .title("Test Title")
                .content("Some content")
                .build();
        when(templateClient.findTemplate(eq("user1"), eq("Test Title")))
                .thenReturn(ResponseEntity.status(HttpStatus.OK).body(templateResponse));

        ClientResponse client = ClientResponse.builder()
                .id(1L)
                .username("user1")
                .email("user1@example.com")
                .phoneNumber("1234567890")
                .preferredCommunicationChannel(PreferredCommunicationChannel.EMAIL)
                .build();
        when(userManagementClient.getClientByUsername("user1"))
                .thenReturn(ResponseEntity.ok(client));

        NotificationResponse expectedResponse = NotificationResponse.builder()
                .clientUsername("user1")
                .senderEmail("sender@example.com")
                .title("Test Title")
                .content("Test Content")
                .status(NotificationStatus.NEW)
                .preferredChannel(PreferredChannel.EMAIL)
                .createdAt(null)
                .message("Notification created")
                .build();
        when(notificationMapper.mapToResponse(request)).thenReturn(expectedResponse);

        NotificationResponse response = notificationService.createNotification(request);
        assertEquals(expectedResponse, response);
        verify(kafkaProducerService).sendEmailTopic(request);
    }

    @Test
    void testCreateNotification_TemplateNotExists_SMS() {
        NotificationRequest request = NotificationRequest.builder()
                .username("user1")
                .senderEmail("sender@example.com")
                .title("Promo")
                .content("Discount available!")
                .status(NotificationStatus.NEW)
                .preferredChannel(PreferredChannel.PHONE)
                .build();

        when(templateClient.findTemplate(eq("user1"), eq("Promo")))
                .thenReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        TemplateResponse createdTemplate = TemplateResponse.builder()
                .clientUsername("user1")
                .title("Promo")
                .content("Discount available!")
                .build();
        when(templateClient.createTemplate(eq("user1"), any())).thenReturn(ResponseEntity.ok(createdTemplate));

        ClientResponse client = ClientResponse.builder()
                .id(1L)
                .username("user1")
                .email("user1@example.com")
                .phoneNumber("1234567890")
                .preferredCommunicationChannel(PreferredCommunicationChannel.SMS)
                .build();
        when(userManagementClient.getClientByUsername("user1")).thenReturn(ResponseEntity.ok(client));

        NotificationResponse expectedResponse = NotificationResponse.builder()
                .clientUsername("user1")
                .senderEmail("sender@example.com")
                .title("Promo")
                .content("Discount available!")
                .status(NotificationStatus.NEW)
                .preferredChannel(PreferredChannel.PHONE)
                .createdAt(null)
                .message("Notification created")
                .build();
        when(notificationMapper.mapToResponse(request)).thenReturn(expectedResponse);

        NotificationResponse response = notificationService.createNotification(request);
        assertEquals(expectedResponse, response);
        verify(kafkaProducerService).sendSmsTopic(request);
    }

    @Test
    void testSetStatus() {
        NotificationRequest request = NotificationRequest.builder()
                .username("user1")
                .senderEmail("sender@example.com")
                .title("Status Update")
                .content("Updated Content")
                .status(NotificationStatus.NEW)
                .preferredChannel(PreferredChannel.EMAIL)
                .build();

        NotificationResponse response = notificationService.setStatus(request, NotificationStatus.SENT);
        assertEquals("user1", response.clientUsername());
        assertEquals("sender@example.com", response.senderEmail());
        assertEquals("Status Update", response.title());
        assertEquals("Updated Content", response.content());
        assertEquals(NotificationStatus.SENT, response.status());
    }
}