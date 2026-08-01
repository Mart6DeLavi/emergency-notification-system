package com.sensa.authenticationservice.controller;

import com.sensa.authenticationservice.dto.AuthResponse;
import com.sensa.authenticationservice.dto.LoginRequest;
import com.sensa.authenticationservice.dto.RegisterRequest;
import com.sensa.authenticationservice.dto.TokenValidationRequest;
import com.sensa.authenticationservice.dto.TokenValidationResponse;
import com.sensa.authenticationservice.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration and authentication API")
public class AuthenticationController {

    private final AuthService authService;

    @Operation(summary = "Register a new user", description = "Creates a new user account with profile data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or email already exists"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Login user", description = "Authenticates user and returns JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid email or password"),
            @ApiResponse(responseCode = "429", description = "Too many requests")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Validate JWT token", description = "Validates a JWT token and returns user info")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token validation result",
                    content = @Content(schema = @Schema(implementation = TokenValidationResponse.class)))
    })
    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validate(@Valid @RequestBody TokenValidationRequest request) {
        TokenValidationResponse response = authService.validate(request.token());
        return ResponseEntity.ok(response);
    }
}
