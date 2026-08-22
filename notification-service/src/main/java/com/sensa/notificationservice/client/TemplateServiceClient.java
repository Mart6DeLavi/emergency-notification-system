package com.sensa.notificationservice.client;

import com.sensa.notificationservice.config.JwtTokenUtils;
import com.sensa.notificationservice.dto.TemplateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class TemplateServiceClient {

    private final RestTemplate restTemplate;
    private final JwtTokenUtils jwtTokenUtils;

    @Value("${services.template-service.url}")
    private String templateServiceUrl;

    public String getTemplateContent(String templateName) {
        String token = jwtTokenUtils.generateServiceToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<TemplateDto> response = restTemplate.exchange(
                templateServiceUrl + "/api/v1/templates/" + templateName,
                HttpMethod.GET,
                entity,
                TemplateDto.class
        );

        TemplateDto template = response.getBody();
        if (template == null) {
            throw new IllegalStateException("Template not found: " + templateName);
        }
        return template.content();
    }
}
