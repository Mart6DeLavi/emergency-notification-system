package com.sensa.notificationservice.dto.kafka;

import com.sensa.notificationservice.model.KafkaAnswers;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor
public class MessageDeliveryServiceAnswerDto {
    private KafkaAnswers kafkaAnswers;

    public MessageDeliveryServiceAnswerDto(KafkaAnswers kafkaAnswers) {
        this.kafkaAnswers = kafkaAnswers;
    }
}
