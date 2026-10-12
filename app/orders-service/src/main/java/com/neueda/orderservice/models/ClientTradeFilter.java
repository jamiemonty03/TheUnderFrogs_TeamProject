package com.neueda.orderservice.models;

import java.time.LocalDate;
import java.util.Set;
import com.neueda.orderservice.enums.OrderSide;


public record ClientTradeFilter(
    String accountId,
    LocalDate from,
    LocalDate to,
    Set<String> symbols,
    OrderSide side
) {}
