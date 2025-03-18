package com.sensa.usermanagementservice.exception;

public class UserNotRegisteredException extends RuntimeException {
    public UserNotRegisteredException() {
        super("Something went wrong while registering the user");
    }
}
