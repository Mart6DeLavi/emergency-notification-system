package com.sensa.notificationservice.mapper;

import com.sensa.notificationservice.dto.NotificationRequest;
import com.sensa.notificationservice.dto.NotificationResponse;
import com.sensa.notificationservice.entity.Notification;
import com.sensa.notificationservice.model.NotificationStatus;
import com.sensa.notificationservice.model.PreferredChannel;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;


class NotificationMapperTest {

    private final NotificationMapper mapper = new NotificationMapper();

    @Test
    void testMapToEntityAndResponse() {
        NotificationRequest request = NotificationRequest.builder()
                .username("user1")
                .senderEmail("sender@example.com")
                .title("Test")
                .content("Content")
                .status(NotificationStatus.NEW)
                .preferredChannel(PreferredChannel.EMAIL)
                .build();

        Notification entity = mapper.mapToEntity(request);
        entity.setCreatedAt(LocalDateTime.now());
        NotificationResponse response = mapper.mapToResponse(entity);

        assertEquals("user1", response.clientUsername());
        assertEquals("sender@example.com", response.senderEmail());
        assertEquals("Test", response.title());
        assertEquals("Content", response.content());
    }

    @Test
    void testMapToResponseFromRequest() {
        NotificationRequest request = NotificationRequest.builder()
                .username("user1")
                .senderEmail("sender@example.com")
                .title("Test")
                .content("Content")
                .status(NotificationStatus.NEW)
                .preferredChannel(PreferredChannel.EMAIL)
                .build();

        NotificationResponse response = mapper.mapToResponse(request);
        assertEquals("sender@example.com", response.senderEmail());
        assertEquals("Test", response.title());
        assertEquals("Content", response.content());
        assertEquals(NotificationStatus.NEW, response.status());
    }
}
