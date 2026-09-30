package com.neueda.orderservice.events;

import java.math.BigDecimal;

import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.enums.OrderStatus;

public record OrderCancelledPayload(
        String orderId,
        String accountId,
        String symbol,
        OrderSide side,
        int quantity,
        BigDecimal fillPrice,
        OrderStatus status,
        String reason) {
}
