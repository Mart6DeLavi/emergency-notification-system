package com.sensa.authenticationservice.kafka;

import com.sensa.authenticationservice.dto.UserAuthenticationAnswer;
import com.sensa.authenticationservice.dto.UserAuthenticationAnswerDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserManagementKafkaConsumer {

    @Async("kafkaTaskExecutor")
    @KafkaListener(topics = "user-management-service-to-authentication-service", groupId = "authentication-group")
    public void consumeUserManagementService(UserAuthenticationAnswerDto dto) {
        Thread consumerThread = new Thread(() -> {
            String threadName = Thread.currentThread().getName();
            log.info("Consumed message: {}, {}", threadName, dto.getAnswer());

            if (dto.getAnswer().equals(UserAuthenticationAnswer.FOUND)) {
                log.info("[{}] User found. Generating token", threadName);
                //TODO: Сделать генерацию токена если всё хорошо
            } else {
                log.error("[{}] User not found. Error...", threadName);
                //TODO: Сделать что-то если пользователь не найден в базе данных
            }


            log.info("[{}] Work ended.", threadName);
        });

        consumerThread.start();
    }
}
