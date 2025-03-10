package com.sensa.notificationservice.client;

import com.sensa.notificationservice.dto.client.ClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-management-service", url = "${services.user-management-service.url}")
public interface UserManagementClient {

    @GetMapping("/{username}")
    ResponseEntity<ClientResponse> getClientByUsername(@PathVariable("username") String username);
}
