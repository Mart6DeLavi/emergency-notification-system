package com.sensa.authenticationservice.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@EnableAsync
@Configuration
public class AsyncExecutorConfig {

    @Bean(name = "kafkaTaskExecutor")
    public Executor kafkaTaskExecutor() {
        return new ThreadPoolTaskExecutor() {{
            setCorePoolSize(5);
            setMaxPoolSize(10);
            setQueueCapacity(100);
            setThreadNamePrefix("KafkaTaskExecutor-");
            initialize();
        }};
    }
}
