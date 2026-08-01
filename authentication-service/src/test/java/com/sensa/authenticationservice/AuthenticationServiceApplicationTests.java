package com.sensa.authenticationservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false",
        "spring.kafka.bootstrap-servers=",
        "spring.cloud.discovery.enabled=false",
        "eureka.client.enabled=false",
        "jwt.secret=test-secret-that-is-long-enough-for-hs256-signature"
})
class AuthenticationServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
