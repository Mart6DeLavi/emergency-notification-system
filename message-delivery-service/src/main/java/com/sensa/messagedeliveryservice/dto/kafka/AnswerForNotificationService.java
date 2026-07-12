package com.sensa.messagedeliveryservice.dto.kafka;


import com.sensa.messagedeliveryservice.model.KafkaAnswers;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor
public class AnswerForNotificationService {
    private KafkaAnswers kafkaAnswers;

    public AnswerForNotificationService(KafkaAnswers kafkaAnswers) {
        this.kafkaAnswers = kafkaAnswers;
    }
}
