package com.neueda.leap.services;

import com.neueda.leap.models.Account;
import com.neueda.leap.models.Instrument;
import com.neueda.leap.models.Order;
import com.neueda.leap.enums.OrderSide;
import com.neueda.leap.exceptions.AccountNotActiveException;
import com.neueda.leap.exceptions.DuplicateOrderException;
import com.neueda.leap.exceptions.InstrumentNotFoundException;
import com.neueda.leap.exceptions.InsufficientFundsException;
import com.neueda.leap.exceptions.InsufficientHoldingsException;
import com.neueda.leap.exceptions.TradingException;
import com.neueda.leap.repositories.PositionRepository;
import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for creating and managing orders with validation and idempotency support.
 * 
 * Responsibilities:
 * - Validate orders against business rules before creation
 * - Prevent duplicate order processing via idempotency keys
 * - Enforce trading constraints
 */
public class OrderService {
    
    private final OrderValidationService validationService;
    private final Map<String, Order> ordersByIdempotencyKey = new ConcurrentHashMap<>();

    public OrderService(PositionRepository positionRepository) {
        if (positionRepository == null) {
            throw new IllegalArgumentException("PositionRepository cannot be null");
        }
        this.validationService = new OrderValidationService(positionRepository);
    }

    private Order createOrder(Order order) throws DuplicateOrderException {
        if (order.getIdempotencyKey() == null || order.getIdempotencyKey().trim().isEmpty()) {
            throw new IllegalArgumentException("Idempotency key is required");
        }
        
        // Check for duplicate idempotency key
        if (ordersByIdempotencyKey.containsKey(order.getIdempotencyKey())) {
            throw new DuplicateOrderException(order.getIdempotencyKey(), "idempotencyKey");
        }
        
        // Store order with idempotency key
        ordersByIdempotencyKey.put(order.getIdempotencyKey(), order);
        return order;
    }

    public Order placeOrder(Account account, Instrument instrument, OrderSide side,
                           BigDecimal quantity, BigDecimal price, String idempotencyKey)
            throws AccountNotActiveException, InstrumentNotFoundException, TradingException,
                   InsufficientFundsException, InsufficientHoldingsException, 
                   DuplicateOrderException {
        
        // Validate all business rules
        validationService.validateOrder(account, instrument, side, quantity, price);
        
        // Convert BigDecimal quantity to int for Order model
        int quantityInt = quantity.intValue();
        
        // Create the order with validated data
        Order order = new Order();
        order.setAccountId(account.getAccountId());
        order.setSymbol(instrument.getSymbol());
        order.setSide(side);
        order.setQuantity(quantityInt);
        order.setPrice(price);
        order.setIdempotencyKey(idempotencyKey);
        
        // Save order with idempotency check
        return createOrder(order);
    }

    public Optional<Order> getOrderByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(ordersByIdempotencyKey.get(idempotencyKey));
    }
}
