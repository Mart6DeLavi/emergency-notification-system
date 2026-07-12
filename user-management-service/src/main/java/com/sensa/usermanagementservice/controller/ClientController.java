package com.sensa.usermanagementservice.controller;

import com.sensa.usermanagementservice.data.entity.Client;
import com.sensa.usermanagementservice.dto.ClientRegistrationDto;
import com.sensa.usermanagementservice.dto.ClientResponse;
import com.sensa.usermanagementservice.exception.ClientRegistrationException;
import com.sensa.usermanagementservice.exception.UserNotFoundException;
import com.sensa.usermanagementservice.service.ClientControllerMethods;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/clients")
public class ClientController {
    private final ClientControllerMethods clientControllerMethods;

    @Operation(summary = "Create client", description = "Register new client in system")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Client successfully registered"),
            @ApiResponse(responseCode = "400", description = "Incorrect registration data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/create")
    public ResponseEntity<ClientResponse> createClient(@RequestBody final ClientRegistrationDto clientRegistrationDto) {
        try {
            ClientResponse clientResponse = clientControllerMethods.addClient(clientRegistrationDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(clientResponse);
        } catch (ClientRegistrationException ex) {
            log.error("Client registration failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception ex) {
            log.error("Unexpected error during client registration: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Operation(summary = "Update client data", description = "Updates information about the client by user name.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The client's data has been successfully updated"),
            @ApiResponse(responseCode = "404", description = "The client was not found"),
            @ApiResponse(responseCode = "400", description = "Data integrity error"),
            @ApiResponse(responseCode = "500", description = "Error on the server")
    })
    @PatchMapping("/{username}/update")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable("username") String username,
            @RequestBody final ClientRegistrationDto clientRegistrationDto
    ) {
        try {
            ClientResponse updatedClient = clientControllerMethods.updateClient(username, clientRegistrationDto);
            return ResponseEntity.ok(updatedClient);
        } catch (UserNotFoundException ex) {
            log.error("Client update failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (DataIntegrityViolationException ex) {
            log.error("Data integrity error during client update: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception ex) {
            log.error("Unexpected error during client update: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Operation(summary = "Get customer data", description = "Returns customer information by username.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client data successfully received"),
            @ApiResponse(responseCode = "404", description = "Client not found"),
            @ApiResponse(responseCode = "500", description = "Error on the server")
    })
    @GetMapping("/{username}")
    public ResponseEntity<ClientResponse> getClientByUsername(@PathVariable("username") String username) {
        try {
            Client client = clientControllerMethods.findClientByUsername(username);
            return ResponseEntity.ok(clientControllerMethods.mapToResponse(client));
        } catch (UserNotFoundException ex) {
            log.error("Client not found: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception ex) {
            log.error("Unexpected error while fetching client: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Operation(summary = "Delete client", description = "Deletes client from the system by username.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Client not found"),
            @ApiResponse(responseCode = "500", description = "Error on the server")
    })
    @DeleteMapping("/{username}/delete")
    public ResponseEntity<String> deleteClient(@PathVariable("username") String username) {
        return clientControllerMethods.deleteClientByUsername(username);
    }

}
