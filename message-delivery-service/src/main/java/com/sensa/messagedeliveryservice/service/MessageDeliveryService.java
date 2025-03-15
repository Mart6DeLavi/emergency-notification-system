package com.sensa.messagedeliveryservice.service;

import com.sensa.messagedeliveryservice.dto.MessageResponse;
import com.sensa.messagedeliveryservice.dto.kafka.NotificationKafkaDelivery;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageDeliveryService {


    @KafkaListener(topics = "notification-service-to-message-delivery-service-mail")
    public MessageResponse sendMessageThroughEmail(NotificationKafkaDelivery notificationKafkaDelivery) {
        return null;
        //TODO: сделать отправку сообщений на почту через Amazon SES
    }

    @KafkaListener(topics = "notification-service-to-message-delivery-service-sms")
    public MessageResponse sendMessageThroughSms(NotificationKafkaDelivery notificationKafkaDelivery) {
        return null;
        //TODO: сделать отравку сообщений через смс Twilio
    }
}
