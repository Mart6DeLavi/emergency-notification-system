package com.sensa.authenticationservice.service;

import com.sensa.authenticationservice.config.security.BasicSecurityConfig;
import com.sensa.authenticationservice.dto.UserAuthenticationAnswer;
import com.sensa.authenticationservice.dto.UserAuthenticationAnswerDto;
import com.sensa.authenticationservice.dto.UserAuthenticationDto;
import com.sensa.authenticationservice.entity.AuthEntity;
import com.sensa.authenticationservice.kafka.UserManagementServiceKafkaProducer;
import com.sensa.authenticationservice.mapper.UserAuthenticationDtoMapper;
import com.sensa.authenticationservice.repository.UserStorageRepository;
import com.sensa.authenticationservice.util.JwtTokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserManagementServiceKafkaProducer kafkaProducer;

    @Mock
    private UserStorageRepository userStorageRepository;

    @Mock
    private JwtTokenUtils jwtTokenUtils;

    @Mock
    private BasicSecurityConfig basicSecurityConfig;

    @Mock
    private RedisRecoveryService redisRecoveryService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthenticationService authenticationService;

    private final UserAuthenticationDto testDto = new UserAuthenticationDto("testUser", "testPassword");

    @BeforeEach
    void setup() {
        lenient().doReturn(new BCryptPasswordEncoder()).when(basicSecurityConfig).passwordEncoder();
        lenient().when(passwordEncoder.encode("testPassword")).thenReturn("encodedTestPassword");
    }

    @Test
    void testSaveUserData() {
        // Статическое мокирование для UserAuthenticationDtoMapper
        try (MockedStatic<UserAuthenticationDtoMapper> mapperMock = mockStatic(UserAuthenticationDtoMapper.class)) {
            AuthEntity entity = new AuthEntity();
            mapperMock.when(() -> UserAuthenticationDtoMapper.toEntity(testDto)).thenReturn(entity);

            String result = authenticationService.saveUserData(testDto);

            assertEquals("User data saved successfully", result);
            verify(userStorageRepository).save(entity);
            verify(kafkaProducer).sendToUserManagementService(testDto);
        }
    }

    @Test
    void testGenerateTokenFound() throws Exception {
        // При FOUND-ответе генерируется токен.
        when(jwtTokenUtils.generateToken(testDto)).thenReturn("dummyToken");

        // Запускаем отдельный поток, который через 100 мс симулирует получение ответа от Kafka.
        new Thread(() -> {
            try {
                Thread.sleep(100);
                // Создаём пустой экземпляр и с помощью Reflection устанавливаем поле answer
                UserAuthenticationAnswerDto responseDto = new UserAuthenticationAnswerDto();
                ReflectionTestUtils.setField(responseDto, "answer", UserAuthenticationAnswer.FOUND);
                authenticationService.processKafkaResponse(responseDto);
            } catch (InterruptedException e) {
                // Игнорируем
            }
        }).start();

        String token = authenticationService.generateToken(testDto);
        assertEquals("dummyToken", token);
        verify(redisRecoveryService).saveToken(testDto.username(), "dummyToken");
    }

    @Test
    void testGenerateTokenNotFound() throws Exception {
        // При ответе NOTEXIST ожидается null.
        new Thread(() -> {
            try {
                Thread.sleep(100);
                UserAuthenticationAnswerDto responseDto = new UserAuthenticationAnswerDto();
                ReflectionTestUtils.setField(responseDto, "answer", UserAuthenticationAnswer.NOTEXIST);
                authenticationService.processKafkaResponse(responseDto);
            } catch (InterruptedException e) {
                // Игнорируем
            }
        }).start();

        String token = authenticationService.generateToken(testDto);
        assertNull(token);
        verify(redisRecoveryService, never()).saveToken(any(), any());
    }

    @Test
    void testGenerateTokenTimeout() throws Exception {
        // Не отправляем ответ, чтобы симулировать таймаут (метод будет ждать 5 секунд).
        long start = System.currentTimeMillis();
        String token = authenticationService.generateToken(testDto);
        long duration = System.currentTimeMillis() - start;
        assertNull(token);
        // Проверяем, что ожидание составило примерно 5 секунд (с допустимой погрешностью).
        assertTrue(duration >= TimeUnit.SECONDS.toMillis(5));
    }
}