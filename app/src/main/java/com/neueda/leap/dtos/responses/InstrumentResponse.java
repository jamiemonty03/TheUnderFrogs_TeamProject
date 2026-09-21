package com.neueda.leap.dtos.responses;

public record InstrumentResponse(
    String symbol,
    String name,
    String assetClass,
    String currency,
    String exchange,
    boolean tradable
) {}
