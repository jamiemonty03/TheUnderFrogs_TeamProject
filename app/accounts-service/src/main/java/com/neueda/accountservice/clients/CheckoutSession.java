package com.neueda.accountservice.clients;

public record CheckoutSession(
    String id,
    String url,
    String paymentStatus,
    String paymentIntentId,
    Long amountTotalCents,
    String currency,
    String accountId
) {
    public boolean isPaid() {
        return "paid".equals(paymentStatus);
    }
}
