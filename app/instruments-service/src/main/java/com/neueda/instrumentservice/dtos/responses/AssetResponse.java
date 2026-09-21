package com.neueda.instrumentservice.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AssetResponse(
    String symbol,
    String name,
    BigDecimal price,
    LocalDateTime tradeDate,
    LocalDateTime createdAt,
    LocalDateTime lastUpdated
) {}
