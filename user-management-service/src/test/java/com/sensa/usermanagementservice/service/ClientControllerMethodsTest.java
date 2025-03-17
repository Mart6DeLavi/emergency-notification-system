package com.sensa.usermanagementservice.service;

import com.sensa.usermanagementservice.data.entity.Client;
import com.sensa.usermanagementservice.data.repository.ClientRepository;
import com.sensa.usermanagementservice.dto.ClientRegistrationDto;
import com.sensa.usermanagementservice.dto.ClientResponse;
import com.sensa.usermanagementservice.exception.ClientRegistrationException;
import com.sensa.usermanagementservice.exception.UserNotFoundException;
import com.sensa.usermanagementservice.mapper.ClientMapper;
import com.sensa.usermanagementservice.model.PreferredCommunicationChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientControllerMethodsTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientMapper mapper;

    @InjectMocks
    private ClientControllerMethods clientControllerMethods;

    @Test
    void testAddClientSuccess() {
        ClientRegistrationDto registrationDto = new ClientRegistrationDto(
                "John", "Doe", "john_doe", "john@example.com", "password",
                "+1234567890", 30, PreferredCommunicationChannel.EMAIL
        );
        when(clientRepository.findClientByUsername("john_doe")).thenReturn(Optional.empty());
        Client clientEntity = new Client();
        when(mapper.toEntity(registrationDto)).thenReturn(clientEntity);
        Client savedClient = new Client();
        savedClient.setId(1L);
        when(clientRepository.save(clientEntity)).thenReturn(savedClient);
        ClientResponse expectedResponse = new ClientResponse(
                1L, "john_doe", "john@example.com", "+1234567890", PreferredCommunicationChannel.EMAIL
        );
        when(mapper.mapToResponse(savedClient)).thenReturn(expectedResponse);

        ClientResponse response = clientControllerMethods.addClient(registrationDto);
        assertEquals(expectedResponse, response);
        verify(clientRepository).findClientByUsername("john_doe");
        verify(mapper).toEntity(registrationDto);
        verify(clientRepository).save(clientEntity);
        verify(mapper).mapToResponse(savedClient);
    }

    @Test
    void testAddClientAlreadyExists() {
        ClientRegistrationDto registrationDto = new ClientRegistrationDto(
                "John", "Doe", "john_doe", "john@example.com", "password",
                "+1234567890", 30, PreferredCommunicationChannel.EMAIL
        );
        when(clientRepository.findClientByUsername("john_doe"))
                .thenReturn(Optional.of(new Client()));

        ClientRegistrationException exception = assertThrows(
                ClientRegistrationException.class,
                () -> clientControllerMethods.addClient(registrationDto)
        );
        assertEquals("User with such username already exists", exception.getMessage());
        verify(clientRepository).findClientByUsername("john_doe");
        verify(mapper, never()).toEntity(any());
        verify(clientRepository, never()).save(any());
    }

    @Test
    void testAddClientDataIntegrityViolation() {
        ClientRegistrationDto registrationDto = new ClientRegistrationDto(
                "John", "Doe", "john_doe", "john@example.com", "password",
                "+1234567890", 30, PreferredCommunicationChannel.EMAIL
        );
        when(clientRepository.findClientByUsername("john_doe")).thenReturn(Optional.empty());
        Client clientEntity = new Client();
        when(mapper.toEntity(registrationDto)).thenReturn(clientEntity);
        when(clientRepository.save(clientEntity))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        ClientRegistrationException exception = assertThrows(
                ClientRegistrationException.class,
                () -> clientControllerMethods.addClient(registrationDto)
        );
        assertEquals("Username already exists", exception.getMessage());
    }

    @Test
    void testUpdateClientSuccess() {
        String username = "john_doe";
        ClientRegistrationDto registrationDto = new ClientRegistrationDto(
                "Jane", "Doe", "john_doe", "jane@example.com", "newpass",
                "+1987654321", 28, PreferredCommunicationChannel.SMS
        );
        Client existingClient = new Client();
        existingClient.setUsername(username);
        when(clientRepository.findClientByUsername(username)).thenReturn(Optional.of(existingClient));

        Client updatedClient = new Client();
        updatedClient.setUsername(username);
        when(clientRepository.saveAndFlush(existingClient)).thenReturn(updatedClient);
        ClientResponse expectedResponse = new ClientResponse(
                1L, "john_doe", "jane@example.com", "+1987654321", PreferredCommunicationChannel.SMS
        );
        when(mapper.mapToResponse(updatedClient)).thenReturn(expectedResponse);

        ClientResponse response = clientControllerMethods.updateClient(username, registrationDto);
        assertEquals(expectedResponse, response);
        verify(clientRepository).findClientByUsername(username);
        verify(clientRepository).saveAndFlush(existingClient);
        verify(mapper).mapToResponse(updatedClient);
    }

    @Test
    void testUpdateClientNotFound() {
        String username = "non_existing";
        ClientRegistrationDto registrationDto = new ClientRegistrationDto(
                "Jane", "Doe", "non_existing", "jane@example.com", "newpass",
                "+1987654321", 28, PreferredCommunicationChannel.SMS
        );
        when(clientRepository.findClientByUsername(username)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> clientControllerMethods.updateClient(username, registrationDto)
        );
        assertEquals(String.format("Client with username %s not found", username), exception.getMessage());
    }

    @Test
    void testUpdateClientDataIntegrityViolation() {
        String username = "john_doe";
        ClientRegistrationDto registrationDto = new ClientRegistrationDto(
                "Jane", "Doe", "john_doe", "jane@example.com", "newpass",
                "+1987654321", 28, PreferredCommunicationChannel.SMS
        );
        Client existingClient = new Client();
        existingClient.setUsername(username);
        when(clientRepository.findClientByUsername(username)).thenReturn(Optional.of(existingClient));
        when(clientRepository.saveAndFlush(existingClient))
                .thenThrow(new DataIntegrityViolationException("violation"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> clientControllerMethods.updateClient(username, registrationDto)
        );
        assertTrue(exception.getMessage().contains("Failed to update client due to data integrity violation"));
    }

    @Test
    void testFindClientByUsernameSuccess() {
        String username = "john_doe";
        Client client = new Client();
        when(clientRepository.findClientByUsername(username)).thenReturn(Optional.of(client));

        Client foundClient = clientControllerMethods.findClientByUsername(username);
        assertEquals(client, foundClient);
        verify(clientRepository).findClientByUsername(username);
    }

    @Test
    void testFindClientByUsernameNotFound() {
        String username = "non_existing";
        when(clientRepository.findClientByUsername(username)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> clientControllerMethods.findClientByUsername(username)
        );
        assertEquals("Client with username " + username + " not found", exception.getMessage());
    }

    @Test
    void testDeleteClientByUsernameSuccess() {
        String username = "john_doe";
        Client client = new Client();
        client.setUsername(username);
        when(clientRepository.findClientByUsername(username)).thenReturn(Optional.of(client));
        doNothing().when(clientRepository).delete(client);

        ResponseEntity<String> response = clientControllerMethods.deleteClientByUsername(username);
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("deleted successfully"));
        verify(clientRepository).delete(client);
    }

    @Test
    void testDeleteClientByUsernameException() {
        String username = "john_doe";
        Client client = new Client();
        client.setUsername(username);
        when(clientRepository.findClientByUsername(username)).thenReturn(Optional.of(client));
        doThrow(new RuntimeException("delete error")).when(clientRepository).delete(client);

        ResponseEntity<String> response = clientControllerMethods.deleteClientByUsername(username);
        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("Something went wrong"));
    }

    @Test
    void testMapToResponse() {
        Client client = new Client();
        ClientResponse response = new ClientResponse(
                1L, "john_doe", "john@example.com", "+1234567890", PreferredCommunicationChannel.EMAIL
        );
        when(mapper.mapToResponse(client)).thenReturn(response);

        ClientResponse result = clientControllerMethods.mapToResponse(client);
        assertEquals(response, result);
        verify(mapper).mapToResponse(client);
    }
}