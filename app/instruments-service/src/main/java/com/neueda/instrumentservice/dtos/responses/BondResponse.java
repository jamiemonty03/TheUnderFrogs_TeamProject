package com.neueda.instrumentservice.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BondResponse(
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
    BigDecimal yieldToMaturity,
    BigDecimal couponRate,
    BigDecimal duration,
    BigDecimal beta,
    BigDecimal ytdReturn,
    BigDecimal threeYearAvgReturn,
    BigDecimal fiveYearAvgReturn,
    BigDecimal distributionYield
) {}
