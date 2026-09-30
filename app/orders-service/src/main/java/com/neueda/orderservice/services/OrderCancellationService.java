package com.neueda.orderservice.services;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neueda.orderservice.enums.OrderStatus;
import com.neueda.orderservice.events.OrderCancelledApplicationEvent;
import com.neueda.orderservice.events.OrderCancelledPayload;
import com.neueda.orderservice.exceptions.OrderNotCancellableException;
import com.neueda.orderservice.exceptions.OrderNotFoundException;
import com.neueda.orderservice.models.Order;
import com.neueda.orderservice.repositories.OrderRepository;

@Service
public class OrderCancellationService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderCancellationService(OrderRepository orderRepository, ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(String orderId) throws OrderNotFoundException, OrderNotCancellableException {
        int updated = orderRepository.updateStatusIfCurrent(
                orderId, OrderStatus.NEW.name(), OrderStatus.CANCELLED.name());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (updated == 0) {
            throw new OrderNotCancellableException(orderId, order.getOrderStatus());
        }

        eventPublisher.publishEvent(new OrderCancelledApplicationEvent(this, new OrderCancelledPayload(
                order.getOrderId(),
                order.getAccountId(),
                order.getSymbol(),
                order.getSide(),
                order.getQuantity(),
                null,
                OrderStatus.CANCELLED,
                "Cancelled by trader")));
    }
}
