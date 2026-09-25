package com.neueda.positionservice.dtos.requests;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreatePositionRequest(
    @NotBlank(message = "Account ID cannot be null or blank")
    String accountId,

    @NotBlank(message = "Symbol cannot be null or blank")
    String symbol,

    @NotNull(message = "Quantity cannot be null")
    @PositiveOrZero(message = "Quantity cannot be negative")
    BigDecimal quantity,

    @NotNull(message = "Average cost cannot be null")
    @PositiveOrZero(message = "Average cost cannot be negative")
    BigDecimal averageCost
) {}
