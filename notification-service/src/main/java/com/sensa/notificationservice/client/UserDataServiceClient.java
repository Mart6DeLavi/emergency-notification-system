package com.sensa.notificationservice.client;

import com.sensa.notificationservice.dto.kafka.UserLocationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserDataServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.user-data-management-service.url}")
    private String userDataServiceUrl;

    public List<UserLocationResponse> getRecipientsByLocation(String city, String street) {
        String url = userDataServiceUrl + "/api/v1/users/location?city=" + city;
        if (street != null && !street.isBlank()) {
            url += "&street=" + street;
        }

        ResponseEntity<UserLocationResponse[]> response =
                restTemplate.getForEntity(url, UserLocationResponse[].class);

        UserLocationResponse[] body = response.getBody();
        return body == null ? List.of() : Arrays.asList(body);
    }
}
