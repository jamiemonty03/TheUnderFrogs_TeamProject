package com.neueda.tradeexecutor.dtos;

import java.math.BigDecimal;
import com.neueda.tradeexecutor.enums.OrderSide;

public record OrderDto(
    String symbol,
    OrderSide side,
    BigDecimal price
) {}
