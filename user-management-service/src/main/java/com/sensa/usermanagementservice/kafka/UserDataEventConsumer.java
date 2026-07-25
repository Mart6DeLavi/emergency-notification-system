package com.sensa.usermanagementservice.kafka;

import com.sensa.usermanagementservice.dto.UserCreateEvent;
import com.sensa.usermanagementservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDataEventConsumer {

    private final UserService userService;

    @KafkaListener(
            topics = "user.registered",
            groupId = "user-data-management-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeUserRegistered(UserCreateEvent event) {
        log.info("Received user.registered event: userId={}", event.userId());
        userService.createFromKafkaEvent(event).subscribe(
                v -> log.info("User created from event: userId={}", event.userId()),
                error -> log.error("Failed to create user from event: userId={}", event.userId(), error)
        );
    }
}
