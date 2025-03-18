package com.sensa.messagedeliveryservice.model;

public enum NotificationStatus{
    NEW("NEW"),
    SENT("SENT"),
    RESENDING("RESENDING"),
    ERROR("ERROR"),
    CORRUPT("CORRUPT");

    private final String value;

    NotificationStatus(String value) {
        this.value = value;
    }
}

