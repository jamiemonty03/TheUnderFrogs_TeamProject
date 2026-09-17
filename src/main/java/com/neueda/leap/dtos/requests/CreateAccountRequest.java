package com.neueda.leap.dtos.requests;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

public record CreateAccountRequest(
    @NotBlank(message = "Holder name is required") String holderName,
    @NotNull(message = "Initial balance is required") @DecimalMin(value = "0", message = "Initial balance must be non-negative") BigDecimal initialBalance
) {}
