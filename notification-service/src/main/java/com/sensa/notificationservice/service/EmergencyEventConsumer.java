package com.sensa.notificationservice.service;

import com.sensa.notificationservice.dto.kafka.EmergencyConfirmedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmergencyEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "emergency.confirmed",
            groupId = "notification-service",
            containerFactory = "emergencyKafkaListenerContainerFactory"
    )
    public void onEmergencyConfirmed(EmergencyConfirmedEvent event) {
        log.info("Received emergency.confirmed: emergencyId={}, template={}, city={}, street={}",
                event.emergencyId(), event.templateName(), event.city(), event.street());
        notificationService.handleEmergencyConfirmed(event);
    }
}
