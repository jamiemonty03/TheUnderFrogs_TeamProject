package com.neueda.accountservice.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateHolderNameRequest(
    @NotBlank(message = "Holder name cannot be null or blank")
    @Size(max = 255, message = "Holder name must be 255 characters or fewer")
    String holderName
) {}
