package com.neueda.tradeexecutor.dtos;

import java.math.BigDecimal;

// resting: the limit isn't reached yet, so the order stays NEW and is checked again on the next price
public record FillDecision(
    boolean filled,
    BigDecimal fillPrice,
    String reason,
    boolean resting
) {

    public static FillDecision fill(BigDecimal fillPrice) {
        return new FillDecision(true, fillPrice, null, false);
    }

    public static FillDecision reject(String reason) {
        return new FillDecision(false, null, reason, false);
    }

    public static FillDecision rest(String reason) {
        return new FillDecision(false, null, reason, true);
    }
}
