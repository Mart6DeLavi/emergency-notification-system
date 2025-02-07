package com.sensa.usermanagementservice.kafka;

import com.sensa.usermanagementservice.dto.UserAuthenticationAnswerDto;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@EnableAsync
@Component
@RequiredArgsConstructor
public class AuthenticationServiceKafkaProducer {

    private final KafkaTemplate<String, UserAuthenticationAnswerDto> kafkaTemplate;
    private final ExecutorService executor;

    @Autowired
    public AuthenticationServiceKafkaProducer(KafkaTemplate<String, UserAuthenticationAnswerDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.executor = Executors.newFixedThreadPool(10, r -> {
           Thread thread = new Thread(r);
           thread.setName("AuthenticationServiceKafkaProducer-" + thread.getId());
           thread.setDaemon(true);
           return thread;
        });
    }

    @Async
    public void sendToAuthenticationService(String topic, UserAuthenticationAnswerDto userAuthenticationAnswerDto) {
        executor.submit(() -> {
           try {
               log.info("Sending UserAuthenticationAnswerDto to Kafka topic {}: {}", topic, userAuthenticationAnswerDto);
               kafkaTemplate.send(topic, userAuthenticationAnswerDto);
           } catch (Exception ex) {
               log.error("Error while sending UserAuthenticationAnswerDto to Kafka topic {}: {}", topic, ex.getMessage());
           }
        });
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException ie) {
                executor.shutdownNow();
            }
        }
    }
}
