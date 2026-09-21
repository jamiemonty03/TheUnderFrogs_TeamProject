package com.neueda.leap.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.neueda.leap.enums.OrderStatus;
import com.neueda.leap.enums.OrderSide;

public record OrderResponse(
    String orderId,
    String accountId,
    String symbol,
    OrderSide side,
    int quantity,
    BigDecimal price,
    OrderStatus orderStatus,
    LocalDateTime createdAt
) {}
