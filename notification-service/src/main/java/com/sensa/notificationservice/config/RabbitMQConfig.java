package com.sensa.notificationservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String DELIVERY_QUEUE = "notification.delivery";

    @Value("${spring.rabbitmq.template.exchange:notification.exchange}")
    private String exchangeName;

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue deliveryQueue() {
        return new Queue(DELIVERY_QUEUE, true);
    }

    @Bean
    public Binding deliveryBinding(Queue deliveryQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(deliveryQueue).to(notificationExchange).with(DELIVERY_QUEUE);
    }
}
