package com.neueda.instrumentservice.dtos.ai;

import java.time.Instant;

public record AssistantSource(Type type, String symbol, String label, String url, Instant asOf) {

    public enum Type { PRICE, NEWS, TREND, FUNDAMENTALS }
}
