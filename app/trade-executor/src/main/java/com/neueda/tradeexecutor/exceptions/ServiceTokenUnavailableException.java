package com.neueda.tradeexecutor.exceptions;

public class ServiceTokenUnavailableException extends RuntimeException {

    public ServiceTokenUnavailableException(String message) {
        super(message);
    }

    public ServiceTokenUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
