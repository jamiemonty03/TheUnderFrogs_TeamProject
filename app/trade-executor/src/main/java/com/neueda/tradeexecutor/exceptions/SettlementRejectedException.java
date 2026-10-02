package com.neueda.tradeexecutor.exceptions;

// A permanent business failure (insufficient funds or holdings, inactive account).
// Retrying won't help, so the saga reverses what moved and rejects the order.
public class SettlementRejectedException extends RuntimeException {
    public SettlementRejectedException(String reason, Throwable cause) {
        super(reason, cause);
    }
}
