package com.sensa.usermanagementservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sensa.usermanagementservice.data.entity.Client;
import com.sensa.usermanagementservice.dto.ClientRegistrationDto;
import com.sensa.usermanagementservice.dto.ClientResponse;
import com.sensa.usermanagementservice.exception.ClientRegistrationException;
import com.sensa.usermanagementservice.exception.UserNotFoundException;
import com.sensa.usermanagementservice.model.PreferredCommunicationChannel;
import com.sensa.usermanagementservice.service.ClientControllerMethods;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ClientController.class)
@AutoConfigureMockMvc(addFilters = false)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientControllerMethods clientControllerMethods;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateClientSuccess() throws Exception {
        ClientRegistrationDto registrationDto = new ClientRegistrationDto(
                "John", "Doe", "john_doe", "john@example.com", "password",
                "+1234567890", 30, PreferredCommunicationChannel.EMAIL
        );
        ClientResponse response = new ClientResponse(
                1L, "john_doe", "john@example.com", "+1234567890", PreferredCommunicationChannel.EMAIL
        );
        when(clientControllerMethods.addClient(any(ClientRegistrationDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/clients/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void testCreateClientRegistrationException() throws Exception {
        ClientRegistrationDto registrationDto = new ClientRegistrationDto(
                "John", "Doe", "john_doe", "john@example.com", "password",
                "+1234567890", 30, PreferredCommunicationChannel.EMAIL
        );
        when(clientControllerMethods.addClient(any(ClientRegistrationDto.class)))
                .thenThrow(new ClientRegistrationException("User with such username already exists"));

        mockMvc.perform(post("/api/v1/clients/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateClientSuccess() throws Exception {
        String username = "john_doe";
        ClientRegistrationDto registrationDto = new ClientRegistrationDto(
                "Jane", "Doe", "john_doe", "jane@example.com", "newpass",
                "+1987654321", 28, PreferredCommunicationChannel.SMS
        );
        ClientResponse response = new ClientResponse(
                1L, "john_doe", "jane@example.com", "+1987654321", PreferredCommunicationChannel.SMS
        );
        when(clientControllerMethods.updateClient(username, registrationDto)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/clients/{username}/update", username)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.phoneNumber").value("+1987654321"));
    }

    @Test
    void testUpdateClientNotFound() throws Exception {
        String username = "non_existing";
        ClientRegistrationDto registrationDto = new ClientRegistrationDto(
                "Jane", "Doe", "non_existing", "jane@example.com", "newpass",
                "+1987654321", 28, PreferredCommunicationChannel.SMS
        );
        when(clientControllerMethods.updateClient(username, registrationDto))
                .thenThrow(new UserNotFoundException("Client with username non_existing not found"));

        mockMvc.perform(patch("/api/v1/clients/{username}/update", username)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetClientByUsernameSuccess() throws Exception {
        String username = "john_doe";
        Client client = new Client();
        ClientResponse response = new ClientResponse(
                1L, "john_doe", "john@example.com", "+1234567890", PreferredCommunicationChannel.EMAIL
        );
        when(clientControllerMethods.findClientByUsername(username)).thenReturn(client);
        when(clientControllerMethods.mapToResponse(client)).thenReturn(response);

        mockMvc.perform(get("/api/v1/clients/{username}", username)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("john_doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"));
    }

    @Test
    void testGetClientByUsernameNotFound() throws Exception {
        String username = "non_existing";
        when(clientControllerMethods.findClientByUsername(username))
                .thenThrow(new UserNotFoundException("Client with username non_existing not found"));

        mockMvc.perform(get("/api/v1/clients/{username}", username)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteClientSuccess() throws Exception {
        String username = "john_doe";
        ResponseEntity<String> responseEntity = ResponseEntity.ok(String.format("Client %s deleted successfully", username));
        when(clientControllerMethods.deleteClientByUsername(username)).thenReturn(responseEntity);

        mockMvc.perform(delete("/api/v1/clients/{username}/delete", username)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(String.format("Client %s deleted successfully", username)));
    }
}
