package com.sensa.notificationservice.client;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@EnableFeignClients(clients = {TemplateClient.class, UserManagementClient.class})
class FeignClientsIntegrationTest {

    @Autowired
    private TemplateClient templateClient;

    @Autowired
    private UserManagementClient userManagementClient;

    @Value("${services.template-service.url}")
    private String templateServiceUrl;

    @Test
    void testTemplateClientBeanLoaded() {
        assertNotNull(templateClient, "TemplateClient должен быть создан");
        assertNotNull(templateServiceUrl, "URL для TemplateClient должен быть задан");
    }

    @Test
    void testUserManagementClientBeanLoaded() {
        assertNotNull(userManagementClient, "UserManagementClient должен быть создан");
    }

    @Configuration
    static class TestConfig {
        // Здесь можно задать дополнительные настройки для интеграционного тестирования Feign
    }
}