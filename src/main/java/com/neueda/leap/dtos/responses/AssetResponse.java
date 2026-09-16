package com.neueda.leap.dto.responses;

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
