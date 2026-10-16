package com.neueda.tradeexecutor.dtos;

import java.math.BigDecimal;
import com.neueda.tradeexecutor.enums.OrderSide;
import com.neueda.tradeexecutor.enums.OrderStatus;

// portfolioId is null when the order is for the account's Main portfolio
public record OrderDto(
    String orderId,
    String accountId,
    String symbol,
    OrderSide side,
    int quantity,
    BigDecimal priceLimit,
    OrderStatus orderStatus,
    String portfolioId
) {
    public OrderDto(String orderId, String accountId, String symbol, OrderSide side, int quantity,
                    BigDecimal priceLimit, OrderStatus orderStatus) {
        this(orderId, accountId, symbol, side, quantity, priceLimit, orderStatus, null);
    }
}
