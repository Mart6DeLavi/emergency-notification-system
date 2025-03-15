package com.sensa.usermanagementservice.service;

import com.sensa.usermanagementservice.data.entity.Client;
import com.sensa.usermanagementservice.data.repository.ClientRepository;
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
}
