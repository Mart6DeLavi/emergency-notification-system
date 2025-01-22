package com.sensa.usermanagementservice.controller;

import com.sensa.usermanagementservice.data.entity.Client;
import com.sensa.usermanagementservice.dto.AdditionalUserInfoUpdateDto;
import com.sensa.usermanagementservice.dto.ClientRegistrationDto;
import com.sensa.usermanagementservice.service.ClientControllerMethods;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clients")
public class ClientController {
    private final ClientControllerMethods clientControllerMethods;

    @GetMapping("/all")
    public List<Client> getAllClients() {
        return clientControllerMethods.getAllClients();
    }

    @GetMapping("/findByEmail")
    public Client getClientByEmail(@RequestParam final String email) {
        return clientControllerMethods.findClientByEmail(email);
    }

    @GetMapping("/findByUsername")
    public Client getClientByUsername(@RequestParam final String username) {
        return clientControllerMethods.findClientByUsername(username);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createClient(@RequestBody final ClientRegistrationDto clientRegistrationDto) {
        return clientControllerMethods.addClient(clientRegistrationDto);
    }

    @PatchMapping("/{id}/update-info")
    public ResponseEntity<?> updateClientAdditionalInfo(
            @PathVariable("id") Long clientId,
            @RequestBody final AdditionalUserInfoUpdateDto additionalUserInfoUpdateDto
    ) {
        try {
            clientControllerMethods.updateClient(clientId, additionalUserInfoUpdateDto);
            return ResponseEntity.ok(String.format("Client additional info updated successfully"));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("Failed to update client additional info: " + ex.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteClient(@RequestParam final String username) {
        return clientControllerMethods.deleteClientByUsername(username);
    }

}
