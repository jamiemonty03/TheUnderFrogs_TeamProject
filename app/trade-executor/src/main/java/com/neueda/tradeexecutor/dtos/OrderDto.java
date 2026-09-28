package com.neueda.tradeexecutor.dtos;

import java.math.BigDecimal;
import com.neueda.tradeexecutor.enums.OrderSide;
import com.neueda.tradeexecutor.enums.OrderStatus;

public record OrderDto(
    String orderId,
    String accountId,
    String symbol,
    OrderSide side,
    int quantity,
    BigDecimal price,
    OrderStatus orderStatus
) {}
