package com.sensa.templateservice.client;

import com.sensa.templateservice.dto.feign.ClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "${services.user-management}")
public interface UserManagementClient {

    @GetMapping(value = "/clients/findByUsername")
    ClientResponse getClientByUsername(@RequestParam final String username);
}
