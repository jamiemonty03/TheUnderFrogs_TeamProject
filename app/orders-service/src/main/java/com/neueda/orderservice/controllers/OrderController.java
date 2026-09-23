package com.neueda.orderservice.controllers;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
import com.neueda.orderservice.exceptions.AccountNotActiveException;
import com.neueda.orderservice.exceptions.InstrumentNotFoundException;
import com.neueda.orderservice.exceptions.TradingException;
import com.neueda.orderservice.exceptions.InsufficientFundsException;
import com.neueda.orderservice.exceptions.InsufficientHoldingsException;
import com.neueda.orderservice.exceptions.InvalidOrderException;
import com.neueda.orderservice.exceptions.DuplicateOrderException;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;
    
    @Value("${service.accounts.url:http://accounts-service:8081/api/accounts}")
    private String accountsServiceUrl;
    
    @Value("${service.instruments.url:http://instruments-service:8081/api/instruments}")
    private String instrumentsServiceUrl;

    public OrderController(OrderService orderService, OrderRepository orderRepository, RestTemplate restTemplate) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<Order> orders = orderRepository.findAll();
        List<OrderResponse> responses = orders.stream()
            .map(this::toOrderResponse)
            .toList();
        return ResponseEntity.ok(responses);
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
    public ResponseEntity<?> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        String accountId = request.accountId();
        String symbol = request.symbol();

        Account account = null;
        Instrument instrument = null;
        
        try {
            account = restTemplate.getForObject(
                accountsServiceUrl + "/{accountId}",
                Account.class,
                accountId
            );
        } catch (RestClientException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Account not found: " + accountId));
        }
        
        try {
            instrument = restTemplate.getForObject(
                instrumentsServiceUrl + "/{symbol}",
                Instrument.class,
                symbol
            );
        } catch (RestClientException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Instrument not found: " + symbol));
        }

        if (account == null || instrument == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Account or Instrument not found"));
        }

        try {
            Order order = orderService.placeOrder(
                account,
                instrument,
                request.side(),
                request.quantity(),
                request.price(),
                request.idempotencyKey()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(toOrderResponse(order));
        } catch (AccountNotActiveException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (InsufficientFundsException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        } catch (DuplicateOrderException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getClass().getSimpleName() + ": " + e.getMessage()));
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
        } else {
            order.setUpdatedBy("SYSTEM");
        }

        order.setLastUpdated(java.time.LocalDateTime.now());
        order.setVersion(order.getVersion() + 1);

        orderRepository.update(order);
        return ResponseEntity.ok(toOrderResponse(order));
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
        order.setUpdatedBy("SYSTEM");

        orderRepository.update(order);
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
