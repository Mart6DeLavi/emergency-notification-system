package com.sensa.notificationservice.client;

import com.sensa.notificationservice.dto.template.TemplateRequest;
import com.sensa.notificationservice.dto.template.TemplateResponse;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "template-service", url = "${services.template-service.url}")
public interface TemplateClient {

    @PostMapping("/api/v1/templates/create")
    ResponseEntity<TemplateResponse> createTemplate(
            @RequestHeader String username,
            @RequestBody @Valid TemplateRequest templateRequest
    );

    @GetMapping("/api/v1/templates/find/{title}")
    ResponseEntity<TemplateResponse> findTemplate(
            @RequestHeader String username,
            @PathVariable("title") @Valid String title
    );
}
