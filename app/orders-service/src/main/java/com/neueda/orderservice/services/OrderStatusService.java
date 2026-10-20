package com.neueda.orderservice.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.neueda.orderservice.enums.OrderStatus;
import com.neueda.orderservice.exceptions.OrderNotFoundException;
import com.neueda.orderservice.exceptions.OrderStatusConflictException;
import com.neueda.orderservice.models.ClientTrade;
import com.neueda.orderservice.models.Order;
import com.neueda.orderservice.repositories.OrderRepository;

@Service
public class OrderStatusService {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusService.class);
    private static final Set<OrderStatus> ALLOWED_FROM_NEW =
            EnumSet.of(OrderStatus.FILLED, OrderStatus.REJECTED, OrderStatus.CANCELLED);

    private static final BigDecimal MIN_FILL_PRICE = new BigDecimal("0.01");

    private final OrderRepository orderRepository;
    private final ClientTradeService clientTradeService;
    private final OrderStatusMetrics statusMetrics;

    public OrderStatusService(OrderRepository orderRepository, ClientTradeService clientTradeService) {
        this(orderRepository, clientTradeService, null);
    }

    @Autowired
    public OrderStatusService(OrderRepository orderRepository, ClientTradeService clientTradeService,
            OrderStatusMetrics statusMetrics) {
        this.orderRepository = orderRepository;
        this.clientTradeService = clientTradeService;
        this.statusMetrics = statusMetrics;
    }

    @Transactional(rollbackFor = Exception.class)
    public Order changeStatus(String orderId, OrderStatus expectedStatus, OrderStatus newStatus,
            BigDecimal fillPrice, String reason)
            throws OrderNotFoundException, OrderStatusConflictException {
        if (expectedStatus != OrderStatus.NEW || !ALLOWED_FROM_NEW.contains(newStatus)) {
            throw new IllegalArgumentException(
                    "Status change " + expectedStatus + " -> " + newStatus + " is not allowed");
        }
        if (newStatus == OrderStatus.FILLED && (fillPrice == null || fillPrice.compareTo(MIN_FILL_PRICE) < 0)) {
            throw new IllegalArgumentException("fillPrice of at least " + MIN_FILL_PRICE + " is required for FILLED");
        }
        if (newStatus != OrderStatus.FILLED && fillPrice != null) {
            throw new IllegalArgumentException("fillPrice is only allowed when newStatus is FILLED");
        }

        int updated = orderRepository.updateStatusIfCurrent(
            orderId, expectedStatus.name(), newStatus.name(), newStatus == OrderStatus.REJECTED ? reason : null);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (updated == 0) {
            throw new OrderStatusConflictException(orderId, expectedStatus, order.getOrderStatus());
        }

        if (newStatus == OrderStatus.FILLED) {
            ClientTrade trade = clientTradeService.saveTrade(new ClientTrade(order.getAccountId(), order.getSymbol(),
                    order.getSide(), BigDecimal.valueOf(order.getQuantity()), fillPrice, LocalDate.now()));
            if (statusMetrics != null) {
                statusMetrics.recordTradeExecuted(order.getSymbol());
            }
            log.info("Order {} recorded as client trade {} at {}", orderId, trade.getTradeId(), fillPrice);
        }

        if (statusMetrics != null) {
            statusMetrics.record(newStatus);
        }
        log.info("Order {} status {} -> {} ({})", orderId, expectedStatus, newStatus, reason);
        return order;
    }
}
