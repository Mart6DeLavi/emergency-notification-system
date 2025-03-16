package com.sensa.usermanagementservice;

import com.sensa.usermanagementservice.data.entity.Client;
import com.sensa.usermanagementservice.data.repository.ClientRepository;
import com.sensa.usermanagementservice.dto.ClientRegistrationDto;
import com.sensa.usermanagementservice.dto.ClientResponse;
import com.sensa.usermanagementservice.exception.ClientRegistrationException;
import com.sensa.usermanagementservice.mapper.ClientMapper;
import com.sensa.usermanagementservice.model.PreferredCommunicationChannel;
import com.sensa.usermanagementservice.service.ClientControllerMethods;
import com.sensa.usermanagementservice.service.ClientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ClientControllerMethodsTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientServiceImpl clientService;

    @Mock
    private ClientMapper clientMapper;

    @InjectMocks
    private ClientControllerMethods clientControllerMethods;

    private Client client;
    private ClientRegistrationDto clientDto;

    @BeforeEach
    void setUp() {
        client = new Client();
        client.setUsername("testUser");
        client.setEmail("test@example.com");

        clientDto = new ClientRegistrationDto(
                "John", "Doe", "testUser", "test@example.com", "password", "+1234567890", 30,
                PreferredCommunicationChannel.EMAIL);
    }

    @Test
    void testGetAllClients() {
        when(clientService.getAllClients()).thenReturn(List.of(client));
        List<Client> clients = clientControllerMethods.getAllClients();
        assertNotNull(clients);
        assertEquals(1, clients.size());
        verify(clientService, times(1)).getAllClients();
    }

    @Test
    void testAddClientSuccess() {
        when(clientRepository.findClientByUsername(clientDto.username())).thenReturn(Optional.empty());
        when(clientMapper.toEntity(clientDto)).thenReturn(client); // Убедимся, что не null
        when(clientRepository.save(any(Client.class))).thenReturn(client); // Исправляем мок, чтобы точно не вернуть null

        ClientResponse response = clientControllerMethods.addClient(clientDto);

        assertNotNull(response, "Response не должен быть null");
        verify(clientRepository, times(1)).save(client);
    }


    @Test
    void testAddClientUsernameExists() {
        when(clientRepository.findClientByUsername(clientDto.username())).thenReturn(Optional.of(client));
        assertThrows(ClientRegistrationException.class, () -> clientControllerMethods.addClient(clientDto));
    }
}
