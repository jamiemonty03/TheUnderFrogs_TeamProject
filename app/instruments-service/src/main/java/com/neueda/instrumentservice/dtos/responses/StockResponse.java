package com.neueda.instrumentservice.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockResponse(
    String symbol,
    String name,
    BigDecimal price,
    LocalDateTime tradeDate,
    LocalDateTime createdAt,
    LocalDateTime lastUpdated,
    String sector,
    String industry,
    String country,
    Integer fullTimeEmployees,
    BigDecimal beta,
    BigDecimal trailingPe,
    BigDecimal forwardPe,
    BigDecimal trailingEps,
    BigDecimal dividendRate,
    BigDecimal payoutRatio,
    BigDecimal priceToBook,
    BigDecimal returnOnEquity,
    BigDecimal marketCap,
    BigDecimal sharesOutstanding,
    BigDecimal totalRevenue,
    String website
) {}
