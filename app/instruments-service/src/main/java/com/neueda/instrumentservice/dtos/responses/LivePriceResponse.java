package com.neueda.instrumentservice.dtos.responses;

import java.math.BigDecimal;
import java.time.Instant;

/** The latest live quote for a symbol: price is the mid of bid and ask. */
public record LivePriceResponse(
    String symbol,
    BigDecimal price,
    BigDecimal bid,
    BigDecimal ask,
    Instant quoteTime
) {}
