package com.neueda.leap.dto.responses;

public record InstrumentResponse(
    String symbol,
    String name,
    String assetClass,
    String currency,
    String exchange,
    boolean tradable
) {}
