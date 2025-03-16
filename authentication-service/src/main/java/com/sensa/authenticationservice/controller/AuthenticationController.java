package com.sensa.authenticationservice.controller;

import com.sensa.authenticationservice.dto.UserAuthenticationDto;
import com.sensa.authenticationservice.entity.AuthEntity;
import com.sensa.authenticationservice.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/authentication")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping("/new")
    public String authentication(@RequestBody UserAuthenticationDto userAuthenticationDto) {
        ResponseEntity.ok(authenticationService.saveUserData(userAuthenticationDto));
        return authenticationService.generateToken(userAuthenticationDto);
    }
}
