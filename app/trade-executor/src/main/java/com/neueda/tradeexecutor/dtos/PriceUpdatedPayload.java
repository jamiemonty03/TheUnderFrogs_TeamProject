package com.neueda.tradeexecutor.dtos;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceUpdatedPayload(
    String symbol,
    BigDecimal price,
    BigDecimal bid,
    BigDecimal ask,
    String currency,
    Instant quoteTime
) {}
