package com.sensa.messagedeliveryservice.client;

import com.sensa.messagedeliveryservice.dto.client.notification.NotificationRequest;
import com.sensa.messagedeliveryservice.dto.client.notification.NotificationResponse;
import com.sensa.messagedeliveryservice.model.NotificationStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", url = "${services.notification-service}")
public interface NotificationClient {

    @PatchMapping("/{status}")
    ResponseEntity<NotificationResponse> updateStatus(
            @RequestBody NotificationRequest request,
            @PathVariable NotificationStatus status);
}
