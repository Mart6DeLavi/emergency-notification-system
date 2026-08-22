package com.sensa.usermanagementservice.controller;

import com.sensa.usermanagementservice.dto.UserLocationResponse;
import com.sensa.usermanagementservice.dto.UserResponse;
import com.sensa.usermanagementservice.dto.UserUpdateRequest;
import com.sensa.usermanagementservice.exception.UserNotFoundException;
import com.sensa.usermanagementservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get user", description = "Returns user data by userId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User data received"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{userId}")
    public Mono<ResponseEntity<UserResponse>> getUser(@PathVariable UUID userId) {
        return userService.getByUserId(userId)
                .map(ResponseEntity::ok)
                .onErrorResume(UserNotFoundException.class,
                        e -> Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Get users by location", description = "Returns users in a given city (and optionally street) for broadcast targeting")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of users in the zone")
    })
    @GetMapping("/location")
    public Flux<UserLocationResponse> getUsersByLocation(
            @RequestParam String city,
            @RequestParam(required = false) String street) {
        return userService.findUsersByLocation(city, street);
    }

    @Operation(summary = "Update user", description = "Updates user data by userId")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User data updated"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{userId}")
    public Mono<ResponseEntity<UserResponse>> updateUser(
            @PathVariable UUID userId,
            @RequestBody UserUpdateRequest request) {
        return userService.update(userId, request)
                .map(ResponseEntity::ok)
                .onErrorResume(UserNotFoundException.class,
                        e -> Mono.just(ResponseEntity.notFound().build()));
    }

    @Operation(summary = "Delete user", description = "Deletes user by userId")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deleted"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{userId}")
    public Mono<ResponseEntity<Void>> deleteUser(@PathVariable UUID userId) {
        return userService.delete(userId)
                .then(Mono.just(ResponseEntity.noContent().<Void>build()))
                .onErrorResume(UserNotFoundException.class,
                        e -> Mono.just(ResponseEntity.notFound().build()));
    }
}
