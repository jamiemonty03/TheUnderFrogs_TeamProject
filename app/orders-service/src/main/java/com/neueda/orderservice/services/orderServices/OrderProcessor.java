package com.neueda.orderservice.services.orderServices;

import java.math.BigDecimal;

import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.events.OrderPlacedApplicationEvent;
import com.neueda.orderservice.events.OrderPlacedPayload;
import com.neueda.orderservice.exceptions.*;
import com.neueda.orderservice.models.Account;
import com.neueda.orderservice.models.Instrument;
import com.neueda.orderservice.models.Order;
import com.neueda.orderservice.services.OrderService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Validates and persists a new order, then registers its event for after-commit publication. */
@Component
public class OrderProcessor {

    private final OrderService orderService;
    private final ApplicationEventPublisher eventPublisher;

    public OrderProcessor(OrderService orderService, ApplicationEventPublisher eventPublisher) {
        if (orderService == null) {
            throw new IllegalArgumentException("OrderService cannot be null");
        }
        if (eventPublisher == null) {
            throw new IllegalArgumentException("ApplicationEventPublisher cannot be null");
        }
        this.orderService = orderService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public OrderResult processOrder(
            Account account,
            Instrument instrument,
            OrderSide side,
            BigDecimal quantity,
            BigDecimal price,
            String idempotencyKey)
            throws AccountNotActiveException, InstrumentNotFoundException, TradingException,
                   InsufficientFundsException, InsufficientHoldingsException, DuplicateOrderException {
        if (side == null) {
            throw new InvalidOrderException("Order side cannot be null");
        }

        Order order = orderService.placeOrder(account, instrument, side, quantity, price, idempotencyKey);
        eventPublisher.publishEvent(new OrderPlacedApplicationEvent(
                this,
                new OrderPlacedPayload(
                        order.getOrderId(),
                        order.getAccountId(),
                        order.getSymbol(),
                        order.getSide(),
                        order.getQuantity(),
                        order.getPrice(),
                        order.getIdempotencyKey())));

        return new OrderResult(true, "Order accepted", null, order);
    }
}
