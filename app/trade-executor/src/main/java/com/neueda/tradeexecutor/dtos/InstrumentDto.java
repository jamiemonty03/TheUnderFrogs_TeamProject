package com.neueda.tradeexecutor.dtos;

public record InstrumentDto(
    String symbol,
    boolean tradable
) {}
