package com.fintech.ledgerservice.exception;

public class DuplicateLedgerEntryException extends RuntimeException {
    public DuplicateLedgerEntryException(String message) {
        super(message);
    }
}

