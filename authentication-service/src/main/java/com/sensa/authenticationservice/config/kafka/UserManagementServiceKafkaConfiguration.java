package com.sensa.authenticationservice.config.kafka;

import com.sensa.authenticationservice.deserializer.UserAuthenticationAnswerDtoDeserializer;
import com.sensa.authenticationservice.dto.UserAuthenticationAnswerDto;
import com.sensa.authenticationservice.dto.UserAuthenticationDto;
import com.sensa.authenticationservice.serializer.UserAuthenticationDtoSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
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
public class UserManagementServiceKafkaConfiguration {

    @Bean
    public ProducerFactory<String, UserAuthenticationDto> userAuthenticationKafkaProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, UserAuthenticationDtoSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, UserAuthenticationDto> userAuthenticationKafkaTemplate() {
        return new KafkaTemplate<>(userAuthenticationKafkaProducerFactory());
    }

    @Bean
    public ConsumerFactory<String, UserAuthenticationAnswerDto> userAuthenticationKafkaConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, UserAuthenticationAnswerDtoDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserAuthenticationAnswerDto> userAuthenticationKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, UserAuthenticationAnswerDto> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(userAuthenticationKafkaConsumerFactory());
        return factory;
    }
}
