package com.neueda.leap.dto.requests;

import jakarta.validation.constraints.NotBlank;

public record CreateInstrumentRequest(
    @NotBlank(message = "Symbol is required") String symbol,
    @NotBlank(message = "Name is required") String name,
    @NotBlank(message = "Asset class is required") String assetClass,
    @NotBlank(message = "Currency is required") String currency,
    @NotBlank(message = "Exchange is required") String exchange,
    boolean tradable
) {}
