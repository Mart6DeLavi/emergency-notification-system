package com.sensa.usermanagementservice;

import com.sensa.usermanagementservice.data.entity.Client;
import com.sensa.usermanagementservice.data.repository.ClientRepository;
import com.sensa.usermanagementservice.service.ClientServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientServiceImpl clientService;

    private Client client1;
    private Client client2;

    @BeforeEach
    void setUp() {
        client1 = new Client();
        client1.setUsername("testUser1");
        client1.setEmail("test1@example.com");

        client2 = new Client();
        client2.setUsername("testUser2");
        client2.setEmail("test2@example.com");
    }

    @Test
    void testGetAllClientsWithData() {
        when(clientRepository.getAllClients()).thenReturn(List.of(client1, client2));
        List<Client> clients = clientService.getAllClients();
        assertNotNull(clients);
        assertEquals(2, clients.size());
        assertEquals("testUser1", clients.get(0).getUsername());
        assertEquals("testUser2", clients.get(1).getUsername());
        verify(clientRepository, times(1)).getAllClients();
    }

    @Test
    void testGetAllClientsEmptyList() {
        when(clientRepository.getAllClients()).thenReturn(new ArrayList<>());
        List<Client> clients = clientService.getAllClients();
        assertNotNull(clients);
        assertTrue(clients.isEmpty());
        verify(clientRepository, times(1)).getAllClients();
    }

    @Test
    void testGetAllClientsNullResponse() {
        when(clientRepository.getAllClients()).thenReturn(null);
        List<Client> clients = clientService.getAllClients();
        assertNull(clients);
        verify(clientRepository, times(1)).getAllClients();
    }
}

