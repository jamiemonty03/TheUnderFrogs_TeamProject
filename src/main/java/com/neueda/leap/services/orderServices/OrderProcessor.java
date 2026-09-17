package com.neueda.leap.services.orderServices;

import java.math.BigDecimal;

import com.neueda.leap.enums.OrderSide;
import com.neueda.leap.exceptions.*;
import com.neueda.leap.models.Account;
import com.neueda.leap.models.Instrument;
import com.neueda.leap.models.Order;
import com.neueda.leap.services.OrderService;

/**
 * Main orchestrator for order execution.
 * 
 * Responsibilities:
 * 1. Delegate order creation and validation to OrderService
 * 2. Dispatch to appropriate strategy (BUY or SELL)
 * 3. Return execution result
 * 
 */
public class OrderProcessor {

    private final OrderService orderService;
    private final BuyOrderStrategy buyStrategy;
    private final SellOrderStrategy sellStrategy;

    public OrderProcessor(
            OrderService orderService,
            BuyOrderStrategy buyStrategy,
            SellOrderStrategy sellStrategy) {
        
        if (orderService == null) {
            throw new IllegalArgumentException("OrderService cannot be null");
        }
        if (buyStrategy == null) {
            throw new IllegalArgumentException("BuyOrderStrategy cannot be null");
        }
        if (sellStrategy == null) {
            throw new IllegalArgumentException("SellOrderStrategy cannot be null");
        }
        
        this.orderService = orderService;
        this.buyStrategy = buyStrategy;
        this.sellStrategy = sellStrategy;
    }

    public OrderResult processOrder(Account account, Instrument instrument, OrderSide side,
            BigDecimal quantity, BigDecimal price, String idempotencyKey) 
            throws AccountNotActiveException, InstrumentNotFoundException, TradingException,
                   InsufficientFundsException, InsufficientHoldingsException, 
                   DuplicateOrderException {
        
        Order order = orderService.placeOrder(account, instrument, side, quantity, price, idempotencyKey);
        
        OrderExecutionStrategy strategy = 
            side == OrderSide.BUY ? buyStrategy : sellStrategy;
        
        return strategy.execute(order, account, instrument);
    }

}


