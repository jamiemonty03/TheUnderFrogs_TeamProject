package com.neueda.orderservice.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.neueda.orderservice.enums.OrderStatus;
import com.neueda.orderservice.enums.OrderSide;

public record OrderResponse(
    String orderId,
    String accountId,
    String symbol,
    OrderSide side,
    int quantity,
    BigDecimal price,
    OrderStatus orderStatus,
    int version,
    LocalDateTime createdAt,
    LocalDateTime lastUpdated,
    String updatedBy
) {}
