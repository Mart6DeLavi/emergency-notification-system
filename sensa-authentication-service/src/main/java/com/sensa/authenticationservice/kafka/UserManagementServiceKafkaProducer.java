package com.sensa.authenticationservice.kafka;

import com.sensa.authenticationservice.dto.UserAuthenticationDto;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserManagementServiceKafkaProducer {
    private static final String USER_MANAGEMENT_TOPIC = "authentication-service-to-user-management-service";

    private ExecutorService executor;
    private final KafkaTemplate<String, UserAuthenticationDto> kafkaTemplate;

    @PostConstruct
    public void init() {
        executor = new ThreadPoolExecutor(
                2, 10,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(100),
                new ThreadFactory() {
                    private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();

                    @Override
                    public Thread newThread(Runnable runnable) {
                        Thread thread = defaultFactory.newThread(runnable);
                        thread.setName("kafkaProducerThread-" + thread.getId());
                        thread.setDaemon(true);
                        return thread;
                    }
                }
        );
    }

    @Async("kafkaTaskExecutor")
    public void sendToUserManagementService(UserAuthenticationDto userAuthenticationDto) {
        executor.submit(() -> {
            try {
                log.info("[Producer] Sending UserAuthenticationDto to kafka: {}", userAuthenticationDto);
                kafkaTemplate.send(USER_MANAGEMENT_TOPIC, userAuthenticationDto).get();
                log.info("[Producer] Message sent to kafka successfully");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.error("[Producer] Thread had been interrupted while sending message to kafka: {}", ex.getMessage());
            } catch (ExecutionException ex) {
                log.error("[Producer] Error while sending message to kafka: {}", ex.getMessage());
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
