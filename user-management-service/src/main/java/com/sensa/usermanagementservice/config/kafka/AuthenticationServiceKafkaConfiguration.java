package com.sensa.usermanagementservice.config.kafka;

import com.sensa.usermanagementservice.deserializer.UserAuthenticationDtoDeserializer;
import com.sensa.usermanagementservice.dto.UserAuthenticationAnswerDto;
import com.sensa.usermanagementservice.dto.UserAuthenticationDto;
import com.sensa.usermanagementservice.serializer.UserAuthenticationAnswerDtoSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class AuthenticationServiceKafkaConfiguration {

    @Bean
    public ProducerFactory<String, UserAuthenticationAnswerDto> authenticationServiceProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, UserAuthenticationAnswerDtoSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, UserAuthenticationAnswerDto> authenticationServiceKafkaTemplate() {
        return new KafkaTemplate<>(authenticationServiceProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, UserAuthenticationDto> authenticationServiceConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, UserAuthenticationDtoDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(),
                new UserAuthenticationDtoDeserializer());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserAuthenticationDto> authenticationServiceKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, UserAuthenticationDto> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(authenticationServiceConsumerFactory());
        return factory;
    }
}
