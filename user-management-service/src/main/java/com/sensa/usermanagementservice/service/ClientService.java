package com.sensa.usermanagementservice.service;

import com.sensa.usermanagementservice.data.entity.Client;

import java.util.List;

public interface ClientService {
    List<Client> getAllClients();
    Client findClientByUsername(String username);
    Client findClientByEmail(String email);
}
