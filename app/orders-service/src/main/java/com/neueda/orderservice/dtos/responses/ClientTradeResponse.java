package com.neueda.orderservice.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.neueda.orderservice.enums.OrderSide;

public record ClientTradeResponse(
    Integer tradeId,
    String accountId,
    String symbol,
    OrderSide tradeType,
    BigDecimal quantity,
    BigDecimal price,
    LocalDate tradeDate,
    int version,
    LocalDateTime createdAt,
    LocalDateTime lastUpdated,
    String updatedBy
) {}
