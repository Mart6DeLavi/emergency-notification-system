package com.sensa.notificationservice.model;

public enum KafkaAnswers {
    SENT("SENT"),
    ERROR("ERROR"),
    RESENDING("RESENDING");

    private final String value;

    KafkaAnswers(String value) {
        this.value = value;
    }
}
