package com.neueda.accountservice.exceptions;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException() {
        super("We couldn't find that payment");
    }
}
