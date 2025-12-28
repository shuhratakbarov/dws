package com.fintech.authservice.exception;

/**
 * Thrown when attempting to register with an existing email.
 */
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}

