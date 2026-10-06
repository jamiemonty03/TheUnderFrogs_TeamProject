package com.neueda.orderservice.controllers;

import static net.logstash.logback.argument.StructuredArguments.kv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.neueda.orderservice.exceptions.InstrumentNotFoundException;
import com.neueda.orderservice.exceptions.OrderNotCancellableException;
import com.neueda.orderservice.exceptions.OrderNotFoundException;
import com.neueda.orderservice.exceptions.TradingException;
import com.neueda.orderservice.services.OrderCancellationService;
import com.neueda.orderservice.dtos.requests.UpdateOrderStatusRequest;
import com.neueda.orderservice.exceptions.OrderStatusConflictException;
import com.neueda.orderservice.services.OrderStatusService;


@RestController
@RequestMapping("/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderProcessor orderProcessor;
    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;
    private final OrderCancellationService orderCancellationService;
    private final OrderStatusService orderStatusService;


    @Value("${service.accounts.url:http://accounts-service:8081/api/accounts}")
    private String accountsServiceUrl;
    
    @Value("${service.instruments.url:http://instruments-service:8081/api/instruments}")
    private String instrumentsServiceUrl;

    public OrderController(OrderProcessor orderProcessor, OrderRepository orderRepository, RestTemplate restTemplate,
            OrderCancellationService orderCancellationService, OrderStatusService orderStatusService) {
        this.orderProcessor = orderProcessor;
        this.orderRepository = orderRepository;
        this.restTemplate = restTemplate;
        this.orderCancellationService = orderCancellationService;
        this.orderStatusService = orderStatusService;
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
        log.info("Order placement received {} {} {} {} {}", kv("accountId", request.accountId()),
            kv("symbol", request.symbol()), kv("side", request.side()), kv("quantity", request.quantity()),
            kv("priceLimit", request.priceLimit()));

        Account account = fetchAccount(request.accountId());
        if (account == null) {
            log.warn("Order rejected {} {} {}", kv("status", 404), kv("errorCode", "ACC-404"),
                kv("accountId", request.accountId()));
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("ACC-404", "Account not found: " + request.accountId()));
        }
        Instrument instrument = fetchInstrument(request.symbol());

        OrderResult result = orderProcessor.processOrder(
            account,
            instrument,
            request.side(),
            request.quantity(),
            request.priceLimit(),
            request.idempotencyKey()
        );
        if (!result.isSuccess()) {
            log.warn("Order rejected {} {} {}", kv("status", 422), kv("errorCode", "ORD-422"),
                kv("reason", result.getMessage()));
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("ORD-422", result.getMessage()));
        }

        Order order = result.getOrder();
        log.info("Order placed {} {} {} {} {} {}", kv("orderId", order.getOrderId()),
            kv("accountId", order.getAccountId()), kv("symbol", order.getSymbol()), kv("side", order.getSide()),
            kv("quantity", order.getQuantity()), kv("orderStatus", order.getOrderStatus()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toOrderResponse(order));
    }

    private Account fetchAccount(String accountId) {
        try {
            return restTemplate.getForObject(accountsServiceUrl + "/{accountId}", Account.class, accountId);
        } catch (RestClientException e) {
            log.warn("Accounts service lookup failed {} {}", kv("accountId", accountId),
                kv("error", e.getClass().getSimpleName()));
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
        if (request.priceLimit() != null) {
            order.setPriceLimit(request.priceLimit());
        }
        if (request.side() != null) {
            order.setSide(request.side());
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

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable String orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request)
            throws OrderNotFoundException, OrderStatusConflictException {
        Order order = orderStatusService.changeStatus(
                orderId, request.expectedStatus(), request.newStatus(), request.reason());
        return ResponseEntity.ok(toOrderResponse(order));
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String orderId)
            throws OrderNotFoundException, OrderNotCancellableException {
        orderCancellationService.cancel(orderId);
        return ResponseEntity.noContent().build();
    }

    private OrderResponse toOrderResponse(Order order) {
        return new OrderResponse(
            order.getOrderId(),
            order.getAccountId(),
            order.getSymbol(),
            order.getSide(),
            order.getQuantity(),
            order.getPriceLimit(),
            order.getOrderStatus(),
            order.getVersion(),
            order.getCreatedAt(),
            order.getLastUpdated(),
            order.getUpdatedBy()
        );
    }
}
