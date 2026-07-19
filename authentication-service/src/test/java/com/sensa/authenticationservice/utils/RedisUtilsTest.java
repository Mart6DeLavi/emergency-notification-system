package com.sensa.authenticationservice.utils;

import com.sensa.authenticationservice.dto.RedisTokenDto;
import com.sensa.authenticationservice.util.RedisUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class RedisUtilsTest {
    private RedisUtils redisUtils;
    private RedisTemplate<String, RedisTokenDto> redisTemplate;
    private ValueOperations<String, RedisTokenDto> valueOperations;
    private RedisConnectionFactory connectionFactory;
    private RedisConnection connection;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        connectionFactory = mock(RedisConnectionFactory.class);
        connection = mock(RedisConnection.class);
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(connection);

        redisUtils = new RedisUtils(redisTemplate);
    }

    @Test
    void testSaveTokenToRedisSuccess() {
        String username = "testUser";
        String token = "dummyToken";

        redisUtils.saveTokenToRedis(username, token);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<RedisTokenDto> valueCaptor = ArgumentCaptor.forClass(RedisTokenDto.class);
        ArgumentCaptor<Duration> durationCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(valueOperations).set(keyCaptor.capture(), valueCaptor.capture(), durationCaptor.capture());
        assertEquals("user: " + username, keyCaptor.getValue());
        RedisTokenDto redisTokenDto = valueCaptor.getValue();
        assertEquals(username, redisTokenDto.getUsername());
        assertEquals(token, redisTokenDto.getToken());
        assertEquals(Duration.ofMinutes(30), durationCaptor.getValue());
    }

    @Test
    void testSaveTokenToRedisDataAccessException() {
        String username = "testUser";
        String token = "dummyToken";

        doThrow(new DataAccessException("Error") {}).when(valueOperations).set(anyString(), any(), any());

        assertDoesNotThrow(() -> redisUtils.saveTokenToRedis(username, token));
    }

    @Test
    void testIsRedisAvailableSuccess() {
        when(connection.ping()).thenReturn("PONG");
        assertTrue(redisUtils.isRedisAvailable());
    }

    @Test
    void testIsRedisAvailableFailure() {
        when(connection.ping()).thenReturn(Arrays.toString("NOPE".getBytes()));
        assertFalse(redisUtils.isRedisAvailable());
    }

    @Test
    void testIsRedisAvailableException() {
        when(connectionFactory.getConnection()).thenThrow(new RuntimeException("Redis down"));
        assertFalse(redisUtils.isRedisAvailable());
    }
}
