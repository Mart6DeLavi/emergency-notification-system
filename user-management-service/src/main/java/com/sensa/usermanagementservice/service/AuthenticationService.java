package com.sensa.usermanagementservice.service;

import com.sensa.usermanagementservice.data.repository.ClientRepository;
import com.sensa.usermanagementservice.dto.UserAuthenticationAnswer;
import com.sensa.usermanagementservice.dto.UserAuthenticationAnswerDto;
import com.sensa.usermanagementservice.dto.UserAuthenticationDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuthenticationService {
    private final ClientRepository clientRepository;

    @Autowired
    public AuthenticationService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public UserAuthenticationAnswerDto processAuthentication(UserAuthenticationDto userAuthenticationDto) {
        log.info("Processing  authentication for user: {}", userAuthenticationDto.getUsername());

        log.info("Checking if user exists in the database...");
        boolean userExist = clientRepository.existsByUsername(userAuthenticationDto.getUsername());
        log.info("User exist: {}", userExist);
        UserAuthenticationAnswer answer = userExist ? UserAuthenticationAnswer.FOUND : UserAuthenticationAnswer.NOTEXIST;
        return new UserAuthenticationAnswerDto(answer);
    }
}
