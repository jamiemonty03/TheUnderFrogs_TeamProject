package com.neueda.tradeexecutor.exceptions;

public class TradeEventPublishException extends RuntimeException {
    public TradeEventPublishException(String orderId, Throwable cause) {
        super("Failed to publish outcome for order " + orderId, cause);
    }
}
