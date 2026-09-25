package com.neueda.orderservice.services.orderServices;

import java.math.BigDecimal;

import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.exceptions.*;
import com.neueda.orderservice.models.Account;
import com.neueda.orderservice.models.Instrument;
import com.neueda.orderservice.models.Order;
import com.neueda.orderservice.services.OrderService;

import org.springframework.stereotype.Component;

/**
 * Main orchestrator for order execution.
 * 
 * Responsibilities:
 * 1. Delegate order creation and validation to OrderService
 * 2. Dispatch to appropriate strategy (BUY or SELL)
 * 3. Return execution result
 * 
 */
@Component
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
        
        if (side == null) {
            throw new InvalidOrderException("Order side cannot be null");
        }
        
        Order order = orderService.placeOrder(account, instrument, side, quantity, price, idempotencyKey);
        
        OrderExecutionStrategy strategy = switch (side) {
            case BUY -> buyStrategy;
            case SELL -> sellStrategy;
        };
        
        OrderResult result = strategy.execute(order, account, instrument);
        orderService.saveOrder(order);
        return result;
    }

}


