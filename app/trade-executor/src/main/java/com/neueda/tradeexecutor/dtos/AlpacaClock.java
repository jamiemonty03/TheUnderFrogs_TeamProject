package com.neueda.tradeexecutor.dtos;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonProperty;

// Alpaca's market clock: whether US equities are trading now, and the next open and close
public record AlpacaClock(
    @JsonProperty("is_open") boolean isOpen,
    @JsonProperty("next_open") Instant nextOpen,
    @JsonProperty("next_close") Instant nextClose
) {}
