package com.sensa.messagedeliveryservice.model;

public enum PreferredChannel {
    EMAIL("EMAIL"),
    PHONE("PHONE");

    private final String value;

    PreferredChannel(String value) {
        this.value = value;
    }
}
