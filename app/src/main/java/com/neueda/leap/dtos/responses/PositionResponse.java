package com.neueda.leap.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PositionResponse(
    String accountId,
    String symbol,
    BigDecimal quantity,
    BigDecimal averageCost,
    LocalDateTime lastUpdated
) {}
