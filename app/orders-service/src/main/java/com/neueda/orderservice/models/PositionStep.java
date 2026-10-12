package com.neueda.orderservice.models;

import java.math.BigDecimal;

/**
 * A list of these shows how a client reached their current position, e.g.
 * BUY 15 → 15, SELL 5 → 10, SELL 5 → 5.
 *
 * @see ClientTrade
 */
public record PositionStep(ClientTrade trade, BigDecimal quantityAfter) {}
