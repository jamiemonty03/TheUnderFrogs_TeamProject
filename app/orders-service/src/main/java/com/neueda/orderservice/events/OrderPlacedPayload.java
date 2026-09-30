package com.neueda.orderservice.events;

import java.math.BigDecimal;

import com.neueda.orderservice.enums.OrderSide;

public record OrderPlacedPayload(
        String orderId,
        String accountId,
        String symbol,
        OrderSide side,
        int quantity,
        BigDecimal price,
        String idempotencyKey) {
}
