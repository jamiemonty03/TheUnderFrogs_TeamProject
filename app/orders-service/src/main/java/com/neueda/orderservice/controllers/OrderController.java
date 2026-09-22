
package com.neueda.orderservice.controllers;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

import com.neueda.orderservice.models.Order;
import com.neueda.orderservice.models.Account;
import com.neueda.orderservice.models.Instrument;
import com.neueda.orderservice.services.OrderService;
import com.neueda.orderservice.repositories.OrderRepository;
import com.neueda.orderservice.dtos.requests.PlaceOrderRequest;
import com.neueda.orderservice.dtos.requests.UpdateOrderRequest;
import com.neueda.orderservice.dtos.responses.OrderResponse;
import com.neueda.orderservice.enums.OrderStatus;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    public OrderController(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable String orderId) {
        Optional<Order> order = orderRepository.findById(orderId);
        return order.map(o -> ResponseEntity.ok(toOrderResponse(o)))
                   .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByAccountId(@PathVariable String accountId) {
        List<Order> orders = orderRepository.findByAccountId(accountId);
        List<OrderResponse> responses = orders.stream()
            .map(this::toOrderResponse)
            .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request) throws Exception {
        try {
            String accountId = request.accountId();
            String symbol = request.symbol();

            // TODO: Fetch Account from AccountService using accountId
            // Account account = accountService.getAccount(accountId);
            Account account = null;

            // TODO: Fetch Instrument from InstrumentService using symbol
            // Instrument instrument = instrumentService.getInstrument(symbol);
            Instrument instrument = null;

            if (account == null || instrument == null) {
                throw new IllegalArgumentException("Account or Instrument not found. Integration with AccountService/InstrumentService required.");
            }

            Order order = orderService.placeOrder(
                account,
                instrument,
                request.side(),
                request.quantity(),
                request.price(),
                request.idempotencyKey()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(toOrderResponse(order));
        } catch (Exception e) {
            throw e;
        }
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<OrderResponse> updateOrder(
            @PathVariable String orderId,
            @Valid @RequestBody UpdateOrderRequest request) {

        Optional<Order> existingOrder = orderRepository.findById(orderId);
        if (existingOrder.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Order order = existingOrder.get();

        if (request.quantity() != null) {
            order.setQuantity(request.quantity());
        }
        if (request.price() != null) {
            order.setPrice(request.price());
        }
        if (request.side() != null) {
            order.setSide(request.side());
        }
        if (request.orderStatus() != null) {
            order.setOrderStatus(request.orderStatus());
        }
        if (request.updatedBy() != null) {
            order.setUpdatedBy(request.updatedBy());
        }

        order.setLastUpdated(java.time.LocalDateTime.now());
        order.setVersion(order.getVersion() + 1);

        Order updatedOrder = orderRepository.save(order);
        return ResponseEntity.ok(toOrderResponse(updatedOrder));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String orderId) {
        Optional<Order> existingOrder = orderRepository.findById(orderId);
        if (existingOrder.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Order order = existingOrder.get();
        order.setOrderStatus(OrderStatus.CANCELLED);
        order.setLastUpdated(java.time.LocalDateTime.now());
        order.setVersion(order.getVersion() + 1);

        orderRepository.save(order);
        return ResponseEntity.noContent().build();
    }

    private OrderResponse toOrderResponse(Order order) {
        return new OrderResponse(
            order.getOrderId(),
            order.getAccountId(),
            order.getSymbol(),
            order.getSide(),
            order.getQuantity(),
            order.getPrice(),
            order.getOrderStatus(),
            order.getVersion(),
            order.getCreatedAt(),
            order.getLastUpdated(),
            order.getUpdatedBy()
        );
    }
}
