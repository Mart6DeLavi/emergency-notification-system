package com.sensa.usermanagementservice.kafka;

import com.sensa.usermanagementservice.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserDataEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendUserUpdated(UserResponse userResponse) {
        String key = userResponse.userId().toString();
        kafkaTemplate.send("user.updated", key, userResponse);
        log.info("Sent user.updated event: userId={}", key);
    }

    public void sendUserDeleted(UUID userId) {
        String key = userId.toString();
        kafkaTemplate.send("user.deleted", key, userId);
        log.info("Sent user.deleted event: userId={}", key);
    }
}
