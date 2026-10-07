package com.neueda.tradeexecutor.dtos;

import java.math.BigDecimal;
import com.neueda.tradeexecutor.enums.OrderSide;

// Payload of an ORDER_PLACED event, as published by orders-service (S7-3)
public record OrderPlacedPayload(
    String orderId,
    String accountId,
    String symbol,
    OrderSide side,
    int quantity,
    BigDecimal priceLimit,
    String idempotencyKey
) {}
