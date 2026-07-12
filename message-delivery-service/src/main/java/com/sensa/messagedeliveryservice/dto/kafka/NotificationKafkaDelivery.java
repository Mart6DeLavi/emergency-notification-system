package com.sensa.messagedeliveryservice.dto.kafka;

import lombok.Builder;

@Builder
public record NotificationKafkaDelivery(
        String clientUsername,
        String producerEmail,
        String title,
        String content
) {
}
