package com.neueda.positionservice.dtos.requests;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BuyRequest(
    @NotNull(message = "Quantity must not be null")
    @Positive(message = "Quantity must be greater than zero")
    Integer quantity,

    @NotNull(message = "Price must not be null")
    @DecimalMin("0.01")
    BigDecimal price
) {}
