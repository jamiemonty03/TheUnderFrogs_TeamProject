package com.neueda.tradeexecutor.dtos;

import java.math.BigDecimal;

public record PriceDto(
    String symbol,
    BigDecimal price
) {}
