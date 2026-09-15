package com.neueda.leap.exceptions;

/**
 * Thrown when an order with the same ID already exists in the system.
 * 
 * Order IDs must be unique. This exception indicates an attempt to create
 * a new order with an ID that is already in use, which could indicate:
 * - A duplicate order submission
 * - An ID collision in the generation system
 * - Data corruption
 */
public class DuplicateOrderException extends TradingException {
    
    /**
     * Constructs a DuplicateOrderException with order ID details.
     * 
     * @param orderId the duplicate order ID
     */
    public DuplicateOrderException(String orderId) {
        super("Order already exists: " + orderId);
    }

    /**
     * Constructs a DuplicateOrderException with a custom message.
     * 
     * @param message detailed error message
     */
    public DuplicateOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
