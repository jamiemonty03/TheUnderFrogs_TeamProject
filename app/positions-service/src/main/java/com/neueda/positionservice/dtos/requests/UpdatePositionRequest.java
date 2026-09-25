package com.neueda.positionservice.dtos.requests;

import java.math.BigDecimal;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdatePositionRequest(
    @Positive(message = "Quantity must be positive")
    BigDecimal quantity,
    @PositiveOrZero(message = "Average cost must be positive or zero")
    BigDecimal averageCost
) {}
