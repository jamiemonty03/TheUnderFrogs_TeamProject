package com.neueda.tradeexecutor.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import com.neueda.tradeexecutor.dtos.FillDecision;
import com.neueda.tradeexecutor.dtos.OrderDto;
import com.neueda.tradeexecutor.enums.OrderSide;
import com.neueda.tradeexecutor.enums.OrderStatus;

class FillRuleTest {

    private static OrderDto order(OrderSide side, String limit) {
        return new OrderDto("ORD-1", "ACC-1", "AAPL", side, 10, new BigDecimal(limit), OrderStatus.NEW);
    }

    // BUY

    @Test
    void buyFillsAtMarketPriceWhenMarketIsBelowLimit() {
        FillDecision decision = FillRule.decide(order(OrderSide.BUY, "150.00"), new BigDecimal("148.50"));

        assertTrue(decision.filled());
        assertEquals(new BigDecimal("148.50"), decision.fillPrice());
        assertNull(decision.reason());
    }

    @Test
    void buyFillsWhenMarketEqualsLimit() {
        FillDecision decision = FillRule.decide(order(OrderSide.BUY, "150.00"), new BigDecimal("150.00"));

        assertTrue(decision.filled());
        assertEquals(new BigDecimal("150.00"), decision.fillPrice());
    }

    @Test
    void buyRejectsWhenMarketIsAboveLimit() {
        FillDecision decision = FillRule.decide(order(OrderSide.BUY, "150.00"), new BigDecimal("150.01"));

        assertFalse(decision.filled());
        assertNull(decision.fillPrice());
        assertEquals("Market price 150.01 is above BUY limit 150.00", decision.reason());
    }

    // SELL

    @Test
    void sellFillsAtMarketPriceWhenMarketIsAboveLimit() {
        FillDecision decision = FillRule.decide(order(OrderSide.SELL, "150.00"), new BigDecimal("152.25"));

        assertTrue(decision.filled());
        assertEquals(new BigDecimal("152.25"), decision.fillPrice());
        assertNull(decision.reason());
    }

    @Test
    void sellFillsWhenMarketEqualsLimit() {
        FillDecision decision = FillRule.decide(order(OrderSide.SELL, "150.00"), new BigDecimal("150.00"));

        assertTrue(decision.filled());
        assertEquals(new BigDecimal("150.00"), decision.fillPrice());
    }

    @Test
    void sellRejectsWhenMarketIsBelowLimit() {
        FillDecision decision = FillRule.decide(order(OrderSide.SELL, "150.00"), new BigDecimal("149.99"));

        assertFalse(decision.filled());
        assertNull(decision.fillPrice());
        assertEquals("Market price 149.99 is below SELL limit 150.00", decision.reason());
    }

    // Missing or invalid market price

    @Test
    void rejectsWhenMarketPriceIsMissing() {
        FillDecision decision = FillRule.decide(order(OrderSide.BUY, "150.00"), null);

        assertFalse(decision.filled());
        assertEquals("No valid market price for AAPL", decision.reason());
    }

    @Test
    void rejectsWhenMarketPriceIsZero() {
        FillDecision decision = FillRule.decide(order(OrderSide.SELL, "150.00"), BigDecimal.ZERO);

        assertFalse(decision.filled());
        assertEquals("No valid market price for AAPL", decision.reason());
    }

    @Test
    void rejectsWhenMarketPriceIsNegative() {
        FillDecision decision = FillRule.decide(order(OrderSide.BUY, "150.00"), new BigDecimal("-1.00"));

        assertFalse(decision.filled());
        assertEquals("No valid market price for AAPL", decision.reason());
    }
}
