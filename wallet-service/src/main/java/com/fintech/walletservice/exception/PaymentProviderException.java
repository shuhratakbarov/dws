package com.fintech.walletservice.exception;

/**
 * Exception thrown when payment provider operations fail.
 */
public class PaymentProviderException extends RuntimeException {

    private String errorCode;
    private String providerName;

    public PaymentProviderException(String message) {
        super(message);
    }

    public PaymentProviderException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public PaymentProviderException(String message, String errorCode, String providerName) {
        super(message);
        this.errorCode = errorCode;
        this.providerName = providerName;
    }

    public PaymentProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getProviderName() {
        return providerName;
    }
}
