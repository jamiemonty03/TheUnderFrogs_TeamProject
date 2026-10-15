package com.neueda.instrumentservice.dtos.responses;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PricePointResponse(
    LocalDate date,
    BigDecimal close,
    BigDecimal movingAvg20,
    BigDecimal movingAvg50
) {}
