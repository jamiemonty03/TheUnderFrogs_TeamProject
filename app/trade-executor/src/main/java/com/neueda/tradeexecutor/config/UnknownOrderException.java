package com.neueda.tradeexecutor.config;

public class UnknownOrderException extends RuntimeException {
    public UnknownOrderException(String orderId, Throwable cause) {
        super("Unknown orderId: " + orderId, cause);
    }
}
