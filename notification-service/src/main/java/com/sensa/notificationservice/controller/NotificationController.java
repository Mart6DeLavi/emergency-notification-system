package com.sensa.notificationservice.controller;

import com.sensa.notificationservice.dto.NotificationRequest;
import com.sensa.notificationservice.dto.NotificationResponse;
import com.sensa.notificationservice.model.NotificationStatus;
import com.sensa.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/create")
    public ResponseEntity<NotificationResponse> createNotification(
            @RequestBody @Valid NotificationRequest notificationRequest
    ) {
        NotificationResponse notificationResponse = notificationService.createNotification(notificationRequest);
        return ResponseEntity.status(CREATED).body(notificationResponse);
    }

    @PatchMapping("/status/{clientUsername}")
    public ResponseEntity<NotificationResponse> updateStatus(
            @PathVariable("clientUsername") String clientUsername,
            @RequestParam("status") NotificationStatus status
    ) {
        NotificationResponse response = notificationService.updateNotificationStatus(clientUsername, status);
        return ResponseEntity.status(OK).body(response);
    }
}
