package com.sensa.authenticationservice.service;

import com.sensa.authenticationservice.dto.UserAuthenticationDto;
import com.sensa.authenticationservice.entity.AuthEntity;
import com.sensa.authenticationservice.kafka.UserManagementServiceKafkaProducer;
import com.sensa.authenticationservice.mapper.AuthEntityMapper;
import com.sensa.authenticationservice.repository.UserStorageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserManagementServiceKafkaProducer userManagementServiceKafkaProducer;
    private final UserStorageRepository userStorageRepository;

    public String saveUserData(AuthEntity authEntity) {
        userStorageRepository.save(authEntity);
        UserAuthenticationDto newUser = AuthEntityMapper.toDto(authEntity);
        userManagementServiceKafkaProducer.sendToUserManagementService(newUser);
        return "User data saved successfully";
    }
}
