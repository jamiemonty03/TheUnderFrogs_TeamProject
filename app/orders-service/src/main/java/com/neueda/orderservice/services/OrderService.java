package com.neueda.orderservice.services;

import com.neueda.orderservice.models.Account;
import com.neueda.orderservice.models.Instrument;
import com.neueda.orderservice.models.Order;
import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.exceptions.AccountNotActiveException;
import com.neueda.orderservice.exceptions.DuplicateOrderException;
import com.neueda.orderservice.exceptions.InstrumentNotFoundException;
import com.neueda.orderservice.exceptions.InsufficientFundsException;
import com.neueda.orderservice.exceptions.InsufficientHoldingsException;
import com.neueda.orderservice.exceptions.InvalidOrderException;
import com.neueda.orderservice.exceptions.TradingException;
import com.neueda.orderservice.repositories.OrderRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OrderService {
    
    private final OrderValidationService validationService;
    private final OrderRepository orderRepository;
    private final Map<String, Order> ordersByIdempotencyKey = new ConcurrentHashMap<>();

    public OrderService(OrderRepository orderRepository) {
        if (orderRepository == null) {
            throw new IllegalArgumentException("OrderRepository cannot be null");
        }
        this.validationService = new OrderValidationService();
        this.orderRepository = orderRepository;
    }

    private synchronized Order createOrder(Order order) throws DuplicateOrderException {
        if (ordersByIdempotencyKey.containsKey(order.getIdempotencyKey())) {
            throw new DuplicateOrderException(order.getIdempotencyKey(), "idempotencyKey");
        }
        orderRepository.save(order);
        ordersByIdempotencyKey.put(order.getIdempotencyKey(), order);
        return order;
    }

    public Order saveOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }
        orderRepository.save(order);
        return order;
    }

    public Order placeOrder(Account account, Instrument instrument, OrderSide side,
                           BigDecimal quantity, BigDecimal price, String idempotencyKey)
            throws AccountNotActiveException, InstrumentNotFoundException, TradingException,
                   InsufficientFundsException, InsufficientHoldingsException,
                   InvalidOrderException, DuplicateOrderException {
        
        validationService.validateOrder(account, instrument, side, quantity, price);
        
        int quantityInt = quantity.intValueExact();

        Order order = new Order(
            UUID.randomUUID().toString(),
            account.getAccountId(),
            instrument.getSymbol(),
            side,
            quantityInt,
            price,
            idempotencyKey
        );
        
        return createOrder(order);
    }

    public Optional<Order> getOrderByIdempotencyKey(String idempotencyKey) {
        return Optional.ofNullable(ordersByIdempotencyKey.get(idempotencyKey));
    }
}
