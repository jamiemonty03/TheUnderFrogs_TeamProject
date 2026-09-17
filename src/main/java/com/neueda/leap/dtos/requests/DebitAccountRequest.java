package com.neueda.leap.dtos.requests;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;

public record DebitAccountRequest(
    @NotBlank(message = "Account ID is required") String accountId,
    @NotNull(message = "Amount is required") @DecimalMin(value = "0.01", message = "Amount must be greater than 0") BigDecimal amount
) {}
