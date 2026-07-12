package com.sensa.usermanagementservice.service;

import com.sensa.usermanagementservice.data.entity.Client;
import com.sensa.usermanagementservice.data.repository.ClientRepository;
import com.sensa.usermanagementservice.dto.ClientRegistrationDto;
import com.sensa.usermanagementservice.exception.ClientRegistrationException;
import com.sensa.usermanagementservice.exception.UserNotFoundException;
import com.sensa.usermanagementservice.mapper.ClientMapper;
import com.sensa.usermanagementservice.model.PreferredCommunicationChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientControllerMethodsNonStandardTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientMapper mapper;

    @InjectMocks
    private ClientControllerMethods clientControllerMethods;

    @Test
    void testAddClientMappingChainReturnsNull() {
        ClientRegistrationDto registrationDto = new ClientRegistrationDto(
                "Alice", "Smith", "alice_smith", "alice@example.com", "password123",
                "+11234567890", 25, PreferredCommunicationChannel.EMAIL
        );
        when(clientRepository.findClientByUsername("alice_smith")).thenReturn(Optional.empty());
        Client clientEntity = new Client();
        when(mapper.toEntity(registrationDto)).thenReturn(clientEntity);
        when(clientRepository.save(clientEntity)).thenReturn(clientEntity);
        when(mapper.mapToResponse(clientEntity)).thenReturn(null);

        ClientRegistrationException exception = assertThrows(
                ClientRegistrationException.class,
                () -> clientControllerMethods.addClient(registrationDto)
        );
        assertEquals("New user registration failed", exception.getMessage());
    }

    @Test
    void testUpdateClientMappingChainReturnsNull() {
        String username = "alice_smith";
        ClientRegistrationDto registrationDto = new ClientRegistrationDto(
                "Alice", "Smith", "alice_smith", "alice_new@example.com", "newpass123",
                "+11234567890", 26, PreferredCommunicationChannel.SMS
        );
        Client existingClient = new Client();
        existingClient.setUsername(username);
        when(clientRepository.findClientByUsername(username)).thenReturn(Optional.of(existingClient));
        when(clientRepository.saveAndFlush(existingClient)).thenReturn(existingClient);
        when(mapper.mapToResponse(existingClient)).thenReturn(null);

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> clientControllerMethods.updateClient(username, registrationDto)
        );
        assertEquals(String.format("Client with username %s not found", username), exception.getMessage());
    }

    @Test
    void testUpdateClientFieldsPartialUpdate() {
        Client client = new Client();
        client.setName("Bob");
        client.setSecondName("Marley");
        client.setUsername("bob_marley");
        client.setEmail("bob@oldmail.com");
        client.setPassword("oldpass");
        client.setPhoneNumber("+1111111111");
        client.setAge(40);
        client.setPreferredCommunicationChannel(PreferredCommunicationChannel.EMAIL);

        ClientRegistrationDto registrationDto = new ClientRegistrationDto(
                null,
                "Marley",
                null,
                "bob@newmail.com",
                null,
                null,
                45,
                PreferredCommunicationChannel.SMS
        );

        Client updatedClient = (Client) ReflectionTestUtils.invokeMethod(
                clientControllerMethods, "updateClientFields", client, registrationDto
        );

        assertEquals("Bob", updatedClient.getName());
        assertEquals("Marley", updatedClient.getSecondName());
        assertEquals("bob_marley", updatedClient.getUsername());
        assertEquals("bob@newmail.com", updatedClient.getEmail());
        assertEquals("oldpass", updatedClient.getPassword());
        assertEquals("+1111111111", updatedClient.getPhoneNumber());
        assertEquals(45, updatedClient.getAge());
        assertEquals(PreferredCommunicationChannel.SMS, updatedClient.getPreferredCommunicationChannel());
    }
}
