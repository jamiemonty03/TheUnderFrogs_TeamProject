package com.neueda.orderservice.models;

import java.time.LocalDate;
import java.util.Set;
import com.neueda.orderservice.enums.OrderSide;

/**
 * Filters for searching client trade history. Every field is optional; a null field
 * doesn't filter, and the filters that are set must all match.
 *
 * from and to are inclusive trade dates. symbols limits results to those symbols,
 * so an empty set matches no trades (e.g. an asset class with no instruments).
 *
 * @see ClientTrade
 */
public record ClientTradeFilter(
    String accountId,
    LocalDate from,
    LocalDate to,
    Set<String> symbols,
    OrderSide side
) {}
