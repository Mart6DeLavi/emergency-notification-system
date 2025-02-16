package com.sensa.usermanagementservice.service;

import com.sensa.usermanagementservice.data.entity.Client;
import com.sensa.usermanagementservice.data.enums.PreferredCommunicationChannel;
import com.sensa.usermanagementservice.data.repository.ClientRepository;
import com.sensa.usermanagementservice.dto.ClientRegistrationDto;
import com.sensa.usermanagementservice.dto.ClientResponse;
import com.sensa.usermanagementservice.exception.ClientRegistrationException;
import com.sensa.usermanagementservice.exception.UserNotFoundException;
import com.sensa.usermanagementservice.mapper.ClientMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientControllerMethods {
    private final ClientRepository clientRepository;
    private final ClientServiceImpl clientService;
    private final ClientMapper mapper;

    public List<Client> getAllClients() {
        return clientService.getAllClients();
    }

    public ClientResponse addClient(ClientRegistrationDto clientRegistrationDto) {
        Optional<Client> existingUser = clientRepository.findClientByUsername(clientRegistrationDto.username());
        if (existingUser.isPresent()) {
            throw new ClientRegistrationException("User with such username already exists");
        }

        try {
            return Optional.of(clientRegistrationDto)
                    .map(mapper::toEntity)
                    .map(clientRepository::save)
                    .map(mapper::mapToResponse)
                    .orElseThrow(() -> new ClientRegistrationException("New user registration failed"));
        } catch (DataIntegrityViolationException ex) {
            throw new ClientRegistrationException("Username already exists");
        }
    }

    public ClientResponse updateClient(String username, ClientRegistrationDto clientRegistrationDto) {
        try {
            return clientRepository.findClientByUsername(username)
                    .map(client -> updateClientFields(client, clientRegistrationDto))
                    .map(clientRepository::saveAndFlush)
                    .map(mapper::mapToResponse)
                    .orElseThrow(() -> new UserNotFoundException(
                            String.format("Client with username %s not found", username)
                    ));
        } catch (DataIntegrityViolationException ex) {
            throw new RuntimeException("Failed to update client due to data integrity violation", ex);
        }
    }

    private Client updateClientFields(Client client, ClientRegistrationDto request) {
        Map<Supplier<Object>, Consumer<Object>> fieldMappings = Map.of(
                request::name, value -> client.setName((String) value),
                request::secondName, value -> client.setSecondName((String) value),
                request::username, value -> client.setUsername((String) value),
                request::email, value -> client.setEmail((String) value),
                request::password, value -> client.setPassword((String) value),
                request::phoneNumber, value -> client.setPhoneNumber((String) value),
                request::age, value -> client.setAge((Integer) value),
                request::preferredCommunicationChannel, value -> client.setPreferredCommunicationChannel((PreferredCommunicationChannel) value)
        );

        fieldMappings.forEach((getter, setter) -> Optional.ofNullable(getter.get()).ifPresent(setter));

        return client;
    }

    public Client findClientByUsername(String username) {
        return clientRepository.findClientByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Client with username " + username + " not found"));
    }

    @Transactional
    public ResponseEntity<String> deleteClientByUsername(String username) {
        try {
            clientRepository.delete(findClientByUsername(username));
            return ResponseEntity.ok(String.format("Client %s deleted successfully", username));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Something went wrong: " + e.getMessage());
        }
    }
    
    public ClientResponse mapToResponse(Client client) {
        return mapper.mapToResponse(client);
    }
}
