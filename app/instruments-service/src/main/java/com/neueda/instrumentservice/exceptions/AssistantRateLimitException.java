package com.neueda.instrumentservice.exceptions;

public class AssistantRateLimitException extends RuntimeException {

    public AssistantRateLimitException(String message) {
        super(message);
    }
}
