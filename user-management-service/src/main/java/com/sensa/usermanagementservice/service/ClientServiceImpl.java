package com.sensa.usermanagementservice.service;

import com.sensa.usermanagementservice.data.entity.Client;
import com.sensa.usermanagementservice.data.repository.ClientRepository;
import com.sensa.usermanagementservice.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;

    @Override
    public List<Client> getAllClients() {
        return clientRepository.getAllClients();
    }

    @Override
    public Client findClientByUsername(String username) {
        return clientRepository.findClientByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    @Override
    public Client findClientByEmail(String email) {
        return clientRepository.findClientByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
    }
}
