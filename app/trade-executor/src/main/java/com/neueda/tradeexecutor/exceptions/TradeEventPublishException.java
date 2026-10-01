package com.neueda.tradeexecutor.exceptions;

// Thrown when trade-events doesn't confirm a publish. It is retryable, so the offset isn't acknowledged.
public class TradeEventPublishException extends RuntimeException {
    public TradeEventPublishException(String orderId, Throwable cause) {
        super("Failed to publish outcome for order " + orderId, cause);
    }
}
