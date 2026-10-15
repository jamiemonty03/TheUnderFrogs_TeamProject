package com.neueda.accountservice.dtos.requests;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record TopUpRequest(
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "10.00", message = "The minimum top-up is $10.00")
    @DecimalMax(value = "10000.00", message = "The maximum top-up is $10,000.00")
    @Digits(integer = 5, fraction = 2, message = "Amount can have at most 2 decimal places")
    BigDecimal amount,

    @Size(max = 200, message = "returnTo is too long")
    String returnTo
) {}
