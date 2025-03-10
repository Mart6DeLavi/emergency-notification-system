package com.sensa.notificationservice.mapper;

import com.sensa.notificationservice.dto.NotificationRequest;
import com.sensa.notificationservice.dto.template.TemplateRequest;
import org.springframework.stereotype.Component;

@Component
public class TemplateMapper {

    public static TemplateRequest mapToRequest(NotificationRequest notificationRequest) {
        return TemplateRequest.builder()
                .clientUsername(notificationRequest.username())
                .title(notificationRequest.title())
                .content(notificationRequest.content())
                .build();
    }
}
