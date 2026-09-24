package com.neueda.positionservice.dtos.requests;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ReplacePositionRequest(
    @NotNull(message = "Quantity cannot be null")
    @PositiveOrZero(message = "Quantity cannot be negative")
    BigDecimal quantity,

    @NotNull(message = "Average cost cannot be null")
    @PositiveOrZero(message = "Average cost cannot be negative")
    BigDecimal averageCost
) {}
