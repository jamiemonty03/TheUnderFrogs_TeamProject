package com.neueda.positionservice.dtos.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SellRequest(
    @NotNull(message = "Quantity must not be null")
    @Positive(message = "Quantity must be greater than zero")
    Integer quantity
) {}
