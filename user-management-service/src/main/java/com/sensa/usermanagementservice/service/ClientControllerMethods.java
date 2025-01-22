package com.sensa.usermanagementservice.service;

import com.sensa.usermanagementservice.data.entity.AdditionalUserInfo;
import com.sensa.usermanagementservice.data.entity.Client;
import com.sensa.usermanagementservice.data.repository.ClientRepository;
import com.sensa.usermanagementservice.dto.AdditionalUserInfoUpdateDto;
import com.sensa.usermanagementservice.dto.ClientRegistrationDto;
import com.sensa.usermanagementservice.exception.UserNotFoundException;
import com.sensa.usermanagementservice.exception.UserNotRegisteredException;
import com.sensa.usermanagementservice.mapper.ClientMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientControllerMethods {
    private final ClientRepository clientRepository;
    private final ClientServiceImpl clientService;
    private final ClientMapper clientMapper;


    public List<Client> getAllClients() {
        return clientService.getAllClients();
    }

    public ResponseEntity<?> addClient(ClientRegistrationDto clientRegistrationDto) {
        try {
            Client client = clientMapper.toEntity(clientRegistrationDto);
            clientRepository.save(client);
            return ResponseEntity.ok(String.format("Client %s added successfully", client.getName()));
        }catch (UserNotRegisteredException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Something went wrong: " + ex.getMessage());
        }catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred" + ex.getMessage());
        }
    }

    public void updateClient(Long clientId, AdditionalUserInfoUpdateDto additionalUserInfoUpdateDto) {
        try {
            var client = clientRepository.findById(clientId)
                    .orElseThrow(() -> new UserNotFoundException(String.format("Client with id %s not found", clientId)));

            AdditionalUserInfo existingInfo = client.getAdditionalUserInfo();
            if (existingInfo == null) {
                existingInfo = new AdditionalUserInfo();
            }

            try {
                if (additionalUserInfoUpdateDto.gender() != null) {
                    existingInfo.setGender(additionalUserInfoUpdateDto.gender());
                }
                if (additionalUserInfoUpdateDto.bloodGroup() != null) {
                    existingInfo.setBloodGroup(additionalUserInfoUpdateDto.bloodGroup());
                }
                if (additionalUserInfoUpdateDto.height() != null) {
                    existingInfo.setHeight(additionalUserInfoUpdateDto.height());
                }
                if (additionalUserInfoUpdateDto.country() != null) {
                    existingInfo.setCountry(additionalUserInfoUpdateDto.country());
                }
                if (additionalUserInfoUpdateDto.city() != null) {
                    existingInfo.setCity(additionalUserInfoUpdateDto.city());
                }
            } catch (Exception e) {
                log.error("Error updating AdditionalUserInfo fields: {}", e.getMessage());
                throw new RuntimeException("Failed to update user info fields", e);
            }

            client.setAdditionalUserInfo(existingInfo);

            try {
                clientRepository.save(client);
            } catch (Exception e) {
                log.error("Error saving updated client: {}", e.getMessage());
                throw new RuntimeException("Failed to save updated client data", e);
            }
        } catch (UserNotFoundException e) {
            log.error("User not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error occurred: {}", e.getMessage());
            throw new RuntimeException("Failed to process user update request", e);
        }
    }

    public Client findClientByUsername(String username) {
        try {
            return clientService.findClientByUsername(username);
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong: " + e.getMessage());
        }
    }

    public Client findClientByEmail(String email) {
        try {
            return clientService.findClientByEmail(email);
        } catch (Exception e) {
            throw new RuntimeException("Something went wrong: " + e.getMessage());
        }
    }

    @Transactional
    public ResponseEntity<?> deleteClientByUsername(String username) {
        try {
            clientRepository.delete(findClientByUsername(username));
            return ResponseEntity.ok(String.format("Client %s deleted successfully", username));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Something went wrong: " + e.getMessage());
        }
    }
}
