package com.sensa.usermanagementservice.mapper;

import com.sensa.usermanagementservice.config.SecurityConfig;
import com.sensa.usermanagementservice.data.entity.Client;
import com.sensa.usermanagementservice.data.enums.Role;
import com.sensa.usermanagementservice.dto.ClientRegistrationDto;
import com.sensa.usermanagementservice.dto.ClientResponse;
import com.sensa.usermanagementservice.exception.UserNotRegisteredException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ClientMapper {

    private final SecurityConfig securityConfig;


    public Client toEntity(ClientRegistrationDto clientRegistrationDto) {
        return Optional.of(clientRegistrationDto)
                .map(dto -> {
                    Client client = new Client();
                    client.setName(dto.name());
                    client.setSecondName(dto.secondName());
                    client.setUsername(dto.username());
                    client.setEmail(dto.email());
                    client.setPassword(securityConfig.passwordEncoder().encode(dto.password()));
                    client.setAge(dto.age());
                    client.setPhoneNumber(dto.phoneNumber());
                    client.setPreferredCommunicationChannel(dto.preferredCommunicationChannel());
                    client.setRole(Role.USER);
                    return client;
                })
                .orElseThrow(UserNotRegisteredException::new);
    }

    public ClientResponse mapToResponse(Client client) {
        if (client == null) {
            throw new IllegalArgumentException("Client cannot be null");
        }

        return new ClientResponse(
                client.getId(),
                client.getUsername(),
                client.getEmail(),
                client.getPhoneNumber(),
                client.getPreferredCommunicationChannel()
        );
    }
}
