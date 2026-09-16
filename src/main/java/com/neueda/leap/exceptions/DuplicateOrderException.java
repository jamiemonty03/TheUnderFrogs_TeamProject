package com.neueda.leap.exceptions;

public class DuplicateOrderException extends TradingException {
    
    public DuplicateOrderException(String orderId) {
        super("Order already exists: " + orderId);
    }

    public DuplicateOrderException(String key, String keyType) {
        super("Duplicate " + keyType + ": " + key);
    }

    public DuplicateOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
