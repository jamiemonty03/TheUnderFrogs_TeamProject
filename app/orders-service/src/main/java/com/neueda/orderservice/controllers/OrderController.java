package com.neueda.orderservice.controllers;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

import com.neueda.orderservice.models.Order;
import com.neueda.orderservice.models.Account;
import com.neueda.orderservice.models.Instrument;
import com.neueda.orderservice.services.orderServices.OrderProcessor;
import com.neueda.orderservice.services.orderServices.OrderResult;
import com.neueda.orderservice.repositories.OrderRepository;
import com.neueda.orderservice.dtos.requests.PlaceOrderRequest;
import com.neueda.orderservice.dtos.requests.UpdateOrderRequest;
import com.neueda.orderservice.dtos.responses.ErrorResponse;
import com.neueda.orderservice.dtos.responses.OrderResponse;
import com.neueda.orderservice.enums.OrderStatus;
import com.neueda.orderservice.exceptions.InstrumentNotFoundException;
import com.neueda.orderservice.exceptions.TradingException;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderProcessor orderProcessor;
    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;
    
    @Value("${service.accounts.url:http://accounts-service:8081/api/accounts}")
    private String accountsServiceUrl;
    
    @Value("${service.instruments.url:http://instruments-service:8081/api/instruments}")
    private String instrumentsServiceUrl;

    public OrderController(OrderProcessor orderProcessor, OrderRepository orderRepository, RestTemplate restTemplate) {
        this.orderProcessor = orderProcessor;
        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
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
        List<Order> orders = orderRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
        List<OrderResponse> responses = orders.stream()
            .map(this::toOrderResponse)
            .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(@Valid @RequestBody PlaceOrderRequest request) throws TradingException {
        Account account = fetchAccount(request.accountId());
        if (account == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("ACC-404", "Account not found: " + request.accountId()));
        }
        Instrument instrument = fetchInstrument(request.symbol());

        OrderResult result = orderProcessor.processOrder(
            account,
            instrument,
            request.side(),
            request.quantity(),
            request.price(),
            request.idempotencyKey()
        );
        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("ORD-422", result.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toOrderResponse(result.getOrder()));
    }

    private Account fetchAccount(String accountId) {
        try {
            return restTemplate.getForObject(accountsServiceUrl + "/{accountId}", Account.class, accountId);
        } catch (RestClientException e) {
            return null;
        }
    }

    private Instrument fetchInstrument(String symbol) throws InstrumentNotFoundException {
        try {
            Instrument instrument = restTemplate.getForObject(instrumentsServiceUrl + "/{symbol}", Instrument.class, symbol);
            if (instrument == null) {
                throw new InstrumentNotFoundException("Instrument not found: " + symbol);
            }
            return instrument;
        } catch (RestClientException e) {
            throw new InstrumentNotFoundException("Instrument not found: " + symbol, e);
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

        orderRepository.save(order);
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
