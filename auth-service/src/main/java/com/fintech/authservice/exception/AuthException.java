package com.fintech.authservice.exception;

/**
 * Base exception for authentication errors.
 */
public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }
}

