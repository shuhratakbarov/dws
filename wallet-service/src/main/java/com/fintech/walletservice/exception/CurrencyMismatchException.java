package com.fintech.walletservice.exception;

public class CurrencyMismatchException extends WalletException {
    public CurrencyMismatchException(String message) {
        super(message);
    }
}