package com.sensa.authenticationservice.config.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sensa.authenticationservice.dto.RedisTokenDto;
import lombok.Getter;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.UnifiedJedis;

@EnableCaching
@Configuration
public class RedisConfig {

    @Getter
    private static final String REDIS_HOST = System.getenv("REDIS_HOST");

    @Getter
    private static final int REDIS_PORT = Integer.parseInt(System.getenv("REDIS_PORT"));

    @Getter
    private static final String REDIS_USER = System.getenv("REDIS_USERNAME");

    @Getter
    private static final String REDIS_PASSWORD = System.getenv("REDIS_PASSWORD");

    JedisClientConfig config = DefaultJedisClientConfig.builder()
            .user(getREDIS_USER())
            .password(getREDIS_PASSWORD())
            .build();

    UnifiedJedis jedis = new UnifiedJedis(
            new HostAndPort(getREDIS_HOST(), getREDIS_PORT()),
            config
    );

    @Bean
    public RedisTemplate<String, RedisTokenDto> redisTemplate(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper) {
        RedisTemplate<String, RedisTokenDto> template = new RedisTemplate<>();
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}
