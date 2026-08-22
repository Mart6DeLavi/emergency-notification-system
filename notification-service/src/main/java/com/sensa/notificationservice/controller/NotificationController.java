package com.sensa.notificationservice.controller;

import com.sensa.notificationservice.dto.NotificationRequest;
import com.sensa.notificationservice.dto.NotificationResponse;
import com.sensa.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications API", description = "Notification management")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Create notification", description = "Creates a new notification and sends it for delivery")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Notification created"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "500", description = "Server error")
    })
    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(
            @AuthenticationPrincipal UUID userId,
            @RequestBody @Valid NotificationRequest request
    ) {
        NotificationResponse response = notificationService.createNotification(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get my notifications", description = "Returns all notifications for current user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications list")
    })
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal UUID userId
    ) {
        return ResponseEntity.ok(notificationService.getMyNotifications(userId));
    }

    @Operation(summary = "Get notification by ID", description = "Returns notification by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notification found"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getNotification(
            @AuthenticationPrincipal UUID userId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(notificationService.getNotification(id, userId));
    }

    @Operation(summary = "Delete notification", description = "Deletes notification by its ID")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Notification deleted"),
            @ApiResponse(responseCode = "404", description = "Notification not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @AuthenticationPrincipal UUID userId,
            @PathVariable Long id
    ) {
        notificationService.deleteNotification(id, userId);
        return ResponseEntity.noContent().build();
    }
}
