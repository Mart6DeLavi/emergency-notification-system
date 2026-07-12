package com.sensa.notificationservice.controller;

import com.sensa.notificationservice.dto.NotificationRequest;
import com.sensa.notificationservice.dto.NotificationResponse;
import com.sensa.notificationservice.model.NotificationStatus;
import com.sensa.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications API", description = "Notification control")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "Create notification",
            description = "Creates a new notification and returns data about it"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Notification successfully created"),
            @ApiResponse(responseCode = "400", description = "Incorrect notification data"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping("/create")
    public ResponseEntity<NotificationResponse> createNotification(
            @RequestBody @Valid NotificationRequest notificationRequest
    ) {
        NotificationResponse notificationResponse = notificationService.createNotification(notificationRequest);
        return ResponseEntity.status(CREATED).body(notificationResponse);
    }

    @Operation(
            summary = "Update notification status",
            description = "Updates the status of an existing notification"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Notification status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Notification not found"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PatchMapping("/{status}")
    public ResponseEntity<NotificationResponse> updateStatus(
            @RequestBody NotificationRequest request,
            @PathVariable NotificationStatus status
    ) {
        NotificationResponse response = notificationService.setStatus(request, status);
        return ResponseEntity.ok(response);
    }
}
