package com.sensa.notificationservice.exception;

public class InvalidNotificationStatusTransitionException extends RuntimeException {
    public InvalidNotificationStatusTransitionException(String message) {
        super(message);
    }
}
