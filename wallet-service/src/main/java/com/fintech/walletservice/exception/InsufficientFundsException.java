package com.fintech.walletservice.exception;

public class InsufficientFundsException extends WalletException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
