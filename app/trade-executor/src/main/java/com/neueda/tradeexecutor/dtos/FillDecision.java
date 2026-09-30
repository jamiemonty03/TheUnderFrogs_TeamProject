package com.neueda.tradeexecutor.dtos;

import java.math.BigDecimal;

public record FillDecision(
    boolean filled,
    BigDecimal fillPrice,
    String reason
) {

    public static FillDecision fill(BigDecimal fillPrice) {
        return new FillDecision(true, fillPrice, null);
    }

    public static FillDecision reject(String reason) {
        return new FillDecision(false, null, reason);
    }
}
