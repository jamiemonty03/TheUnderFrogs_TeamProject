package com.neueda.accountservice.exceptions;

public class PaymentsUnavailableException extends RuntimeException {

    public PaymentsUnavailableException() {
        super("Adding funds isn't available right now");
    }
}
