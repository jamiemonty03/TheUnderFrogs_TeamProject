package com.neueda.accountservice.clients;

public interface PaymentGateway {

    boolean isAvailable();

    CheckoutSession createCheckout(CheckoutRequest request);

    CheckoutSession retrieveCheckout(String sessionId);
}
