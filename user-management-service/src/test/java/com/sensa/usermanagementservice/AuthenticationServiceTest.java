package com.sensa.usermanagementservice;

import com.sensa.usermanagementservice.data.repository.ClientRepository;
import com.sensa.usermanagementservice.dto.UserAuthenticationAnswer;
import com.sensa.usermanagementservice.dto.UserAuthenticationAnswerDto;
import com.sensa.usermanagementservice.dto.UserAuthenticationDto;
import com.sensa.usermanagementservice.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private AuthenticationService authenticationService;

    private UserAuthenticationDto validUser;
    private UserAuthenticationDto invalidUser;

    @BeforeEach
    void setUp() {
        validUser = new UserAuthenticationDto("existingUser", "password123");
        invalidUser = new UserAuthenticationDto("nonExistentUser", "password123");
    }

    @Test
    void testProcessAuthentication_UserExists() {
        when(clientRepository.existsByUsername(validUser.username())).thenReturn(true);
        UserAuthenticationAnswerDto response = authenticationService.processAuthentication(validUser);
        assertNotNull(response);
        assertEquals(UserAuthenticationAnswer.FOUND, response.getUserAuthenticationAnswer());
        verify(clientRepository, times(1)).existsByUsername(validUser.username());
    }

    @Test
    void testProcessAuthentication_UserNotExists() {
        when(clientRepository.existsByUsername(invalidUser.username())).thenReturn(false);
        UserAuthenticationAnswerDto response = authenticationService.processAuthentication(invalidUser);
        assertNotNull(response);
        assertEquals(UserAuthenticationAnswer.NOTEXIST, response.getUserAuthenticationAnswer());
        verify(clientRepository, times(1)).existsByUsername(invalidUser.username());
    }

    @Test
    void testProcessAuthentication_NullUsername() {
        UserAuthenticationDto nullUser = new UserAuthenticationDto("", "password123");
        UserAuthenticationAnswerDto response = authenticationService.processAuthentication(nullUser);
        assertNotNull(response);
        assertEquals(UserAuthenticationAnswer.NOTEXIST, response.getUserAuthenticationAnswer());
    }
}
