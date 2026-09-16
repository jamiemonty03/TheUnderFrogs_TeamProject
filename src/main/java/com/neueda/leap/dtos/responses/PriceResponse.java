package com.neueda.leap.dto.responses;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PriceResponse(
    String symbol,
    LocalDate tradeDate,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    BigDecimal volume
) {}
