package com.fintech.ledgerservice.exception;

public class LedgerEntryNotFoundException extends RuntimeException {
    public LedgerEntryNotFoundException(String message) {
        super(message);
    }
}

