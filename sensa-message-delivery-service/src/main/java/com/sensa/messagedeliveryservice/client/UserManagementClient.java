package com.sensa.messagedeliveryservice.client;

import com.sensa.messagedeliveryservice.dto.client.management.ClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-management-service", url = "${services.user-management-service}")
public interface UserManagementClient {

    @GetMapping("/{username}")
    ResponseEntity<ClientResponse> getClientByUsername(@PathVariable("username") String username);
}