package com.neueda.accountservice.clients;

public record CheckoutRequest(String accountId, long amountCents, String successUrl, String cancelUrl) {}
