package com.fintech.walletservice.exception;

public class DuplicateWalletException extends WalletException {
    public DuplicateWalletException(String message) {
        super(message);
    }
}
