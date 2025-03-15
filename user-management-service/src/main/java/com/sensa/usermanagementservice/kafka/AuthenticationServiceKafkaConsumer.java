package com.sensa.usermanagementservice.kafka;

import com.sensa.usermanagementservice.dto.UserAuthenticationAnswerDto;
import com.sensa.usermanagementservice.dto.UserAuthenticationDto;
import com.sensa.usermanagementservice.service.AuthenticationService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@EnableAsync
public class AuthenticationServiceKafkaConsumer {

    private final ExecutorService executor;
    private final AuthenticationService authenticationService;
    private final AuthenticationServiceKafkaProducer authenticationServiceKafkaProducer;

    private static final String AUTHENTICATION_SERVICE_TOPIC = "user-management-to-authentication-service";

    @Autowired
    public AuthenticationServiceKafkaConsumer(AuthenticationService authenticationService, AuthenticationServiceKafkaProducer authenticationServiceKafkaProducer) {
        this.authenticationService = authenticationService;
        this.authenticationServiceKafkaProducer = authenticationServiceKafkaProducer;
        this.executor = Executors.newFixedThreadPool(10, r -> {
           Thread thread = new Thread(r);
           thread.setName("AuthenticationServiceKafkaConsumer-" + thread.getId());
           thread.setDaemon(true);
           return thread;
        });
    }

    @KafkaListener(topics = "authentication-service-to-user-management-service",
            groupId = "user-management-service",
            containerFactory = "authenticationServiceKafkaListenerContainerFactory"
    )
    public void consume(UserAuthenticationDto dto) {
        executor.submit(() -> {
           log.info("Received UserAuthenticationDto: {}", dto.username());

            UserAuthenticationAnswerDto response = authenticationService.processAuthentication(dto);

            authenticationServiceKafkaProducer.sendToAuthenticationService(AUTHENTICATION_SERVICE_TOPIC, response);
            log.info("Sent authentication result: {}", response.getUserAuthenticationAnswer());
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
            } catch (InterruptedException ex) {
                executor.shutdownNow();
            }
        }
    }
}
