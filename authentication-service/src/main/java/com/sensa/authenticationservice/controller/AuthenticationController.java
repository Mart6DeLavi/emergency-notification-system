package com.sensa.authenticationservice.controller;

import com.sensa.authenticationservice.entity.AuthEntity;
import com.sensa.authenticationservice.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping("/new")
    public ResponseEntity<?> authentication(final AuthEntity authEntity) {
        return ResponseEntity.ok(authenticationService.saveUserData(authEntity));
    }
}
