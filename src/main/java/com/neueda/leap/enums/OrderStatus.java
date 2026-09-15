package com.neueda.leap.enums;

/**
 * Order status enumeration.
 * 
 * NEW: Order created, awaiting processing
 * FILLED: Order successfully executed
 * REJECTED: Order failed validation (insufficient funds, holdings, etc.)
 * CANCELLED: Order cancelled by user
 */
public enum OrderStatus {
    NEW,
    FILLED,
    REJECTED,
    CANCELLED
}
