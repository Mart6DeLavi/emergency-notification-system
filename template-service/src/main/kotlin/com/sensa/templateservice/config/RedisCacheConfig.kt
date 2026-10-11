package com.sensa.templateservice.config

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.time.Duration

@Configuration
@EnableCaching
class RedisCacheConfig {

    @Bean
    fun redisCacheCustomizer(objectMapper: ObjectMapper): RedisCacheManagerBuilderCustomizer {
        val cacheMapper = objectMapper.copy()
        cacheMapper.activateDefaultTyping(
            cacheMapper.polymorphicTypeValidator,
            ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE,
            JsonTypeInfo.As.PROPERTY
        )

        return RedisCacheManagerBuilderCustomizer { builder ->
            builder.cacheDefaults(
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(10))
                    .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer())
                    )
                    .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                            GenericJackson2JsonRedisSerializer(cacheMapper)
                        )
                    )
            )
        }
    }
}
