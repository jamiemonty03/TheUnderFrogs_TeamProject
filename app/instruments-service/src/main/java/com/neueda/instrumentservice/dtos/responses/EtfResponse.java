package com.neueda.instrumentservice.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EtfResponse(
    String symbol,
    String name,
    BigDecimal price,
    LocalDateTime tradeDate,
    LocalDateTime createdAt,
    LocalDateTime lastUpdated,
    String category,
    String fundFamily,
    String legalType,
    BigDecimal netExpenseRatio,
    BigDecimal navPrice,
    BigDecimal totalAssets,
    BigDecimal netAssets,
    BigDecimal ytdReturn,
    BigDecimal threeYearAvgReturn,
    BigDecimal fiveYearAvgReturn,
    BigDecimal beta3Year,
    BigDecimal distributionYield
) {}
