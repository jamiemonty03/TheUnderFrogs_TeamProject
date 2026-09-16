package com.neueda.leap.dto.requests;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

public record CreatePriceRequest(
    @NotBlank(message = "Symbol is required") String symbol,
    LocalDate tradeDate,
    @NotNull(message = "Open price is required") @DecimalMin(value = "0", message = "Open price must be non-negative") BigDecimal open,
    @NotNull(message = "High price is required") @DecimalMin(value = "0", message = "High price must be non-negative") BigDecimal high,
    @NotNull(message = "Low price is required") @DecimalMin(value = "0", message = "Low price must be non-negative") BigDecimal low,
    @NotNull(message = "Close price is required") @DecimalMin(value = "0", message = "Close price must be non-negative") BigDecimal close,
    @NotNull(message = "Volume is required") @DecimalMin(value = "0", message = "Volume must be non-negative") BigDecimal volume
) {}
