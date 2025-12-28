package com.fintech.walletservice.exception;

/**
 * Thrown when a user tries to access a resource they don't own.
 */
public class UnauthorizedAccessException extends WalletException {

    public UnauthorizedAccessException(String message) {
        super(message);
    }
}

