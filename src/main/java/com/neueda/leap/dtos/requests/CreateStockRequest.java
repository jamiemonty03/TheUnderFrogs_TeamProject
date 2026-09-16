package com.neueda.leap.dto.requests;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;

public record CreateStockRequest(
    @NotBlank(message = "Symbol is required") String symbol,
    @NotBlank(message = "Name is required") String name,
    BigDecimal price,
    LocalDateTime tradeDate,
    String sector,
    String industry,
    String country,
    String website
) {}

