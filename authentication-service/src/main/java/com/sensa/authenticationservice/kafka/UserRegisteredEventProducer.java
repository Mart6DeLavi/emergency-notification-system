package com.sensa.authenticationservice.kafka;

import com.sensa.authenticationservice.dto.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRegisteredEventProducer {

    private static final String USER_REGISTERED_TOPIC = "user.registered";

    private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

    public void sendUserRegisteredEvent(UserRegisteredEvent event) {
        kafkaTemplate.send(USER_REGISTERED_TOPIC, event.userId().toString(), event)
                .thenAccept(result -> log.info("Sent user.registered event for userId={}", event.userId()))
                .exceptionally(ex -> {
                    log.error("Failed to send user.registered event: {}", ex.getMessage());
                    return null;
                });
    }
}
