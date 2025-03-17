package com.sensa.notificationservice.service;

import com.sensa.notificationservice.dto.NotificationRequest;
import com.sensa.notificationservice.dto.kafka.NotificationKafkaDelivery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, NotificationKafkaDelivery> kafkaTemplate;

    @InjectMocks
    private KafkaProducerService kafkaProducerService;

    @BeforeEach
    void setup() {
        // Задаем топики через Reflection, чтобы не зависеть от properties
        ReflectionTestUtils.setField(kafkaProducerService, "emailTopic", "email-topic");
        ReflectionTestUtils.setField(kafkaProducerService, "smsTopic", "sms-topic");
    }

    @Test
    void testSendEmailTopic() {
        NotificationRequest request = NotificationRequest.builder()
                .username("user1")
                .senderEmail("sender@example.com")
                .title("Email Title")
                .content("Email Content")
                .status(null)
                .preferredChannel(null)
                .build();

        kafkaProducerService.sendEmailTopic(request);

        ArgumentCaptor<NotificationKafkaDelivery> captor = ArgumentCaptor.forClass(NotificationKafkaDelivery.class);
        verify(kafkaTemplate).send(eq("email-topic"), captor.capture());
        NotificationKafkaDelivery message = captor.getValue();
        assertEquals("user1", message.clientUsername());
        assertEquals("sender@example.com", message.producerEmail());
        assertEquals("Email Title", message.title());
        assertEquals("Email Content", message.content());
    }

    @Test
    void testSendSmsTopic() {
        NotificationRequest request = NotificationRequest.builder()
                .username("user1")
                .senderEmail("sender@example.com")
                .title("SMS Title")
                .content("SMS Content")
                .status(null)
                .preferredChannel(null)
                .build();

        kafkaProducerService.sendSmsTopic(request);

        ArgumentCaptor<NotificationKafkaDelivery> captor = ArgumentCaptor.forClass(NotificationKafkaDelivery.class);
        verify(kafkaTemplate).send(eq("sms-topic"), captor.capture());
        NotificationKafkaDelivery message = captor.getValue();
        assertEquals("user1", message.clientUsername());
        assertEquals("sender@example.com", message.producerEmail());
        assertEquals("SMS Title", message.title());
        assertEquals("SMS Content", message.content());
    }
}
