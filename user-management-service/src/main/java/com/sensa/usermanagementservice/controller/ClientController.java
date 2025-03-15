package com.sensa.usermanagementservice.controller;

import com.sensa.usermanagementservice.dto.ClientRegistrationDto;
import com.sensa.usermanagementservice.dto.ClientResponse;
import com.sensa.usermanagementservice.exception.ClientRegistrationException;
import com.sensa.usermanagementservice.exception.UserNotFoundException;
import com.sensa.usermanagementservice.service.ClientControllerMethods;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Stream;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/clients")
public class ClientController {
    private final ClientControllerMethods clientControllerMethods;

    @PostMapping("/create")
    public ResponseEntity<ClientResponse> createClient(@RequestBody final ClientRegistrationDto clientRegistrationDto) {
        return Stream.of(clientRegistrationDto)
                .map(dto -> {
                    try {
                        return clientControllerMethods.addClient(dto);
                    } catch (ClientRegistrationException ex) {
                        log.error("Client registration failed: {}", ex.getMessage());
                        throw new RuntimeException("BAD_REQUEST");
                    } catch (Exception ex) {
                        log.error("Unexpected error during client registration", ex);
                        throw new RuntimeException("INTERNAL_ERROR");
                    }
                })
                .map(clientResponse -> ResponseEntity.status(HttpStatus.CREATED).body(clientResponse))
                .findFirst()
                .orElseGet(() -> ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    }

    @PatchMapping("/{username}/update")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable("username") String username,
            @RequestBody final ClientRegistrationDto clientRegistrationDto
    ) {
        return Stream.of(username)
                .map(user -> {
                    try {
                        return clientControllerMethods.updateClient(user, clientRegistrationDto);
                    } catch (UserNotFoundException ex) {
                        log.error("Client update failed: {}", ex.getMessage());
                        throw new RuntimeException("NOT_FOUND");
                    } catch (DataIntegrityViolationException ex) {
                        log.error("Data integrity error during client update: {}", ex.getMessage());
                        throw new RuntimeException("BAD_REQUEST");
                    } catch (Exception ex) {
                        log.error("Unexpected error during client update: {}", ex.getMessage());
                        throw new RuntimeException("INTERNAL_ERROR");
                    }
                })
                .map(ResponseEntity::ok)
                .findFirst()
                .orElseGet(() -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }


    @GetMapping("/{username}")
    public ResponseEntity<ClientResponse> getClientByUsername(@PathVariable("username") String username) {
        return Stream.of(username)
                .map(user -> {
                    try {
                        return clientControllerMethods.findClientByUsername(user);
                    } catch (UserNotFoundException ex) {
                        log.error("Client not found: {}", ex.getMessage());
                        throw new RuntimeException("NOT_FOUND");
                    } catch (Exception ex) {
                        log.error("Unexpected error while fetching client: {}", ex.getMessage());
                        throw new RuntimeException("INTERNAL_ERROR");
                    }
                })
                .map(clientControllerMethods::mapToResponse)
                .map(ResponseEntity::ok)
                .findFirst()
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }


    @DeleteMapping("/{username}/delete")
    public ResponseEntity<String> deleteClient(@PathVariable("username") String username) {
        return Stream.of(username)
                .map(clientControllerMethods::deleteClientByUsername)
                .findFirst()
                .orElseGet(() -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete client"));
    }


}
