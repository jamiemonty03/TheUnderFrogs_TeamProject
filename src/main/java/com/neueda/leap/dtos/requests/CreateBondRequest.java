package com.neueda.leap.dto.requests;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;

public record CreateBondRequest(
    @NotBlank(message = "Symbol is required") String symbol,
    @NotBlank(message = "Name is required") String name,
    BigDecimal price,
    LocalDateTime tradeDate,
    String category,
    String fundFamily,
    String legalType
) {}

