package com.neueda.accountservice.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateAccountRequest(
    @NotNull(message = "User ID is required") @Positive(message = "User ID must be positive") Long userId,
    @NotBlank(message = "Holder name is required") String holderName
) {}
