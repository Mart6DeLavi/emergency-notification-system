package com.sensa.authenticationservice.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StartupListener {

//    @PostConstruct
//    public void logRedisConfig() {
//        log.info("🔍 REDIS CONFIG -> Host: {}", System.getenv("REDIS_HOST"));
//        log.info("🔍 REDIS CONFIG -> Port: {}", System.getenv("REDIS_PORT"));
//        log.info("🔍 REDIS CONFIG -> Username: {}", System.getenv("REDIS_USERNAME"));
//        log.info("🔍 REDIS CONFIG -> Password: {}", System.getenv("REDIS_PASSWORD"));
//    }
}
