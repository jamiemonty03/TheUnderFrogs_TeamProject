package com.neueda.instrumentservice.dtos.responses;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceSnapshot(
        String symbol,
        BigDecimal price,
        BigDecimal previousClose,
        BigDecimal changePercent,
        Source priceSource,
        Instant asOf) {

    public enum Source { LIVE, DAILY_CLOSE }
}
