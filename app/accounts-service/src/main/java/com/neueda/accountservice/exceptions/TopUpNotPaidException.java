package com.neueda.accountservice.exceptions;

public class TopUpNotPaidException extends RuntimeException {

    public TopUpNotPaidException(String message) {
        super(message);
    }
}
