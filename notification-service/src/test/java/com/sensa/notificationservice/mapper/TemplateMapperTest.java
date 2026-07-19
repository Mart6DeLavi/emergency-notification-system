package com.sensa.notificationservice.mapper;

import com.sensa.notificationservice.dto.NotificationRequest;
import com.sensa.notificationservice.dto.template.TemplateRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TemplateMapperTest {

    @Test
    void testMapToRequest() {
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .username("user1")
                .senderEmail("sender@example.com")
                .title("Title")
                .content("Content")
                .status(null)
                .preferredChannel(null)
                .build();

        TemplateRequest templateRequest = TemplateMapper.mapToRequest(notificationRequest);
        assertEquals("user1", templateRequest.clientUsername());
        assertEquals("Title", templateRequest.title());
        assertEquals("Content", templateRequest.content());
    }
}
