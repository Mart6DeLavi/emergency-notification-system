package com.sensa.templateservice.exception;

public class TemplateAlreadyExistException extends RuntimeException {
    public TemplateAlreadyExistException(String message) {
        super(message);
    }
}
