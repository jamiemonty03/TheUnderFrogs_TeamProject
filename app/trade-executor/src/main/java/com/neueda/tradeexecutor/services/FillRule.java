package com.neueda.tradeexecutor.services;

import java.math.BigDecimal;
import com.neueda.tradeexecutor.dtos.FillDecision;
import com.neueda.tradeexecutor.dtos.OrderDto;
import com.neueda.tradeexecutor.enums.OrderSide;

public final class FillRule {

    private FillRule() {}

    public static FillDecision decide(OrderDto order, BigDecimal marketPrice) {
        if (marketPrice == null || marketPrice.signum() <= 0) {
            return FillDecision.reject("No valid market price for " + order.symbol());
        }

        BigDecimal limit = order.priceLimit();

        if (order.side() == OrderSide.BUY) {
            if (marketPrice.compareTo(limit) <= 0) {
                return FillDecision.fill(marketPrice);
            }
            return FillDecision.reject("Market price " + marketPrice + " is above BUY limit " + limit);
        }

        if (marketPrice.compareTo(limit) >= 0) {
            return FillDecision.fill(marketPrice);
        }
        return FillDecision.reject("Market price " + marketPrice + " is below SELL limit " + limit);
    }
}
