package com.neueda.leap.exceptions;

/**
 * Thrown when an order with the same ID or idempotency key already exists.
 */
public class DuplicateOrderException extends TradingException {
    
    /**
     * Constructs a DuplicateOrderException with order ID details.
     */
    public DuplicateOrderException(String orderId) {
        super("Order already exists: " + orderId);
    }

    /**
     * Constructs a DuplicateOrderException for duplicate idempotency key.
     */
    public DuplicateOrderException(String key, String keyType) {
        super("Duplicate " + keyType + ": " + key);
    }

    /**
     * Constructs a DuplicateOrderException with a custom message.
     */
    public DuplicateOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
