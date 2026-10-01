package com.neueda.orderservice.controllers;

import com.neueda.orderservice.models.Order;
import com.neueda.orderservice.repositories.OrderRepository;
import com.neueda.orderservice.services.orderServices.OrderProcessor;
import com.neueda.orderservice.dtos.requests.PlaceOrderRequest;
import com.neueda.orderservice.dtos.requests.UpdateOrderRequest;
import com.neueda.orderservice.enums.AccountStatus;
import com.neueda.orderservice.models.Account;
import com.neueda.orderservice.models.Instrument;
import com.neueda.orderservice.services.orderServices.OrderResult;
import com.neueda.orderservice.dtos.responses.ErrorResponse;
import com.neueda.orderservice.dtos.responses.OrderResponse;
import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.enums.OrderStatus;
import com.neueda.orderservice.exceptions.OrderNotCancellableException;
import com.neueda.orderservice.exceptions.OrderNotFoundException;
import com.neueda.orderservice.services.OrderCancellationService;
import com.neueda.orderservice.services.OrderStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OrderController Unit Tests
 * 
 * Tests are written to work with InMemoryOrderRepository for fast unit testing.
 * See Docker Integration Test section below for conversion to real PostgreSQL database.
 */
@DisplayName("OrderController Unit Tests")
public class OrderControllerTest {

    private OrderController orderController;
    
    @Mock
    private OrderRepository orderRepository;
    
    @Mock
    private RestTemplate restTemplate;
    
    @Mock
    private OrderProcessor orderProcessor;

    @Mock
    private OrderCancellationService orderCancellationService;

    @Mock
    private OrderStatusService orderStatusService;

    private Order testOrder1;
    private Order testOrder2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderController = new OrderController(orderProcessor, orderRepository, restTemplate, orderCancellationService, orderStatusService);

        testOrder1 = new Order(
            "ORD001",
            "ACC001",
            "AAPL",
            OrderSide.BUY,
            100,
            new BigDecimal("150.25"),
            "idempotent-key-1"
        );

        testOrder2 = new Order(
            "ORD002",
            "ACC001",
            "MSFT",
            OrderSide.SELL,
            50,
            new BigDecimal("300.00"),
            "idempotent-key-2"
        );

        when(orderRepository.findById("ORD001")).thenReturn(Optional.of(testOrder1));
        when(orderRepository.findById("ORD002")).thenReturn(Optional.of(testOrder2));
        when(orderRepository.findById("NONEXISTENT")).thenReturn(Optional.empty());
        when(orderRepository.findByAccountIdOrderByCreatedAtDesc("ACC001")).thenReturn(java.util.List.of(testOrder1, testOrder2));
        when(orderRepository.findByAccountIdOrderByCreatedAtDesc("ACC999")).thenReturn(java.util.List.of());
    }

    @Test
    @DisplayName("GET /orders/{orderId} returns order when found")
    void testGetOrderByIdSuccess() {
        var response = orderController.getOrderById("ORD001");
        
        assertTrue(response.getStatusCode().is2xxSuccessful());
        OrderResponse order = response.getBody();
        assertEquals("ORD001", order.orderId());
        assertEquals("AAPL", order.symbol());
        assertEquals(OrderSide.BUY, order.side());
        assertEquals(100, order.quantity());
        assertEquals(new BigDecimal("150.25"), order.price());
        assertEquals(OrderStatus.NEW, order.orderStatus());
        assertEquals("ACC001", order.accountId());
    }

    @Test
    @DisplayName("GET /orders/{orderId} returns 404 when order not found")
    void testGetOrderByIdNotFound() {
        var response = orderController.getOrderById("NONEXISTENT");
        assertTrue(response.getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("GET /orders/account/{accountId} returns all orders for account")
    void testGetOrdersByAccountIdSuccess() {
        var response = orderController.getOrdersByAccountId("ACC001");
        
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(2, response.getBody().size());
        assertEquals("ORD001", response.getBody().get(0).orderId());
        assertEquals("ORD002", response.getBody().get(1).orderId());
        assertTrue(response.getBody().stream().allMatch(o -> "ACC001".equals(o.accountId())));
    }

    @Test
    @DisplayName("GET /orders/account/{accountId} returns empty list for unknown account")
    void testGetOrdersByAccountIdEmpty() {
        var response = orderController.getOrdersByAccountId("ACC999");
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertTrue(response.getBody().isEmpty());
    }

    @Test
    @DisplayName("PUT /orders/{orderId} updates quantity successfully")
    void testUpdateOrderQuantitySuccess() {
        UpdateOrderRequest updateRequest = new UpdateOrderRequest(200, null, null, null, null);
        
        var response = orderController.updateOrder("ORD001", updateRequest);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(200, response.getBody().quantity());
    }

    @Test
    @DisplayName("PUT /orders/{orderId} updates price successfully")
    void testUpdateOrderPriceSuccess() {
        UpdateOrderRequest updateRequest = new UpdateOrderRequest(null, new BigDecimal("160.50"), null, null, null);
        
        var response = orderController.updateOrder("ORD001", updateRequest);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(new BigDecimal("160.50"), response.getBody().price());
    }

    @Test
    @DisplayName("PUT /orders/{orderId} does not change order status")
    void testUpdateOrderDoesNotChangeStatus() {
        UpdateOrderRequest updateRequest = new UpdateOrderRequest(null, null, null, OrderStatus.FILLED, "system");

        var response = orderController.updateOrder("ORD001", updateRequest);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(OrderStatus.NEW, response.getBody().orderStatus());
        assertEquals("system", response.getBody().updatedBy());
    }

    @Test
    @DisplayName("PUT /orders/{orderId} partial update (only some fields)")
    void testUpdateOrderPartialUpdate() {
        UpdateOrderRequest updateRequest = new UpdateOrderRequest(150, new BigDecimal("155.00"), null, null, null);
        
        var response = orderController.updateOrder("ORD001", updateRequest);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(150, response.getBody().quantity());
        assertEquals(new BigDecimal("155.00"), response.getBody().price());
        assertEquals(OrderSide.BUY, response.getBody().side());
        assertEquals(OrderStatus.NEW, response.getBody().orderStatus());
    }

    @Test
    @DisplayName("PUT /orders/{orderId} increments version on update")
    void testUpdateOrderVersionIncrement() {
        int initialVersion = testOrder1.getVersion();
        UpdateOrderRequest updateRequest = new UpdateOrderRequest(120, null, null, null, null);
        
        var response = orderController.updateOrder("ORD001", updateRequest);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(initialVersion + 1, response.getBody().version());
    }

    @Test
    @DisplayName("PUT /orders/{orderId} updates lastUpdated timestamp")
    void testUpdateOrderLastUpdatedTimestamp() {
        UpdateOrderRequest updateRequest = new UpdateOrderRequest(120, null, null, null, null);
        
        var response = orderController.updateOrder("ORD001", updateRequest);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody().lastUpdated());
    }

    @Test
    @DisplayName("PUT /orders/{orderId} returns 404 for non-existent order")
    void testUpdateOrderNotFound() {
        UpdateOrderRequest updateRequest = new UpdateOrderRequest(200, null, null, null, null);
        
        var response = orderController.updateOrder("NONEXISTENT", updateRequest);
        assertTrue(response.getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("DELETE /orders/{orderId} cancels through the cancellation service and returns 204")
    void testDeleteOrderSuccess() throws Exception {
        var response = orderController.deleteOrder("ORD001");

        assertEquals(204, response.getStatusCode().value());
        verify(orderCancellationService).cancel("ORD001");
    }

    @Test
    @DisplayName("DELETE /orders/{orderId} passes a not-found order up to the exception handler (404)")
    void testDeleteOrderNotFound() throws Exception {
        doThrow(new OrderNotFoundException("NONEXISTENT")).when(orderCancellationService).cancel("NONEXISTENT");

        assertThrows(OrderNotFoundException.class, () -> orderController.deleteOrder("NONEXISTENT"));
    }

    @Test
    @DisplayName("DELETE /orders/{orderId} passes an order that is no longer NEW up to the exception handler (409)")
    void testDeleteOrderNotCancellable() throws Exception {
        doThrow(new OrderNotCancellableException("ORD001", OrderStatus.FILLED))
                .when(orderCancellationService).cancel("ORD001");

        assertThrows(OrderNotCancellableException.class, () -> orderController.deleteOrder("ORD001"));
    }

    @Test
    @DisplayName("POST /orders returns 201 with the FILLED order when execution succeeds")
    void testPlaceOrderFilled() throws Exception {
        Account account = new Account("ACC001", "John Doe", new BigDecimal("50000.00"), AccountStatus.ACTIVE);
        Instrument instrument = new Instrument("AAPL", "Apple", new BigDecimal("150.25"), true);
        when(restTemplate.getForObject(anyString(), eq(Account.class), eq("ACC001"))).thenReturn(account);
        when(restTemplate.getForObject(anyString(), eq(Instrument.class), eq("AAPL"))).thenReturn(instrument);
        testOrder1.setOrderStatus(OrderStatus.FILLED);
        when(orderProcessor.processOrder(account, instrument, OrderSide.BUY, new BigDecimal("100"),
                new BigDecimal("150.25"), "idempotent-key-1"))
            .thenReturn(new OrderResult(true, "filled", null, testOrder1));

        var response = orderController.placeOrder(new PlaceOrderRequest("ACC001", "AAPL", OrderSide.BUY,
            new BigDecimal("100"), new BigDecimal("150.25"), "idempotent-key-1"));

        assertEquals(201, response.getStatusCode().value());
        OrderResponse body = (OrderResponse) response.getBody();
        assertEquals("ORD001", body.orderId());
        assertEquals(OrderStatus.FILLED, body.orderStatus());
    }

    @Test
    @DisplayName("POST /orders returns 422 ORD-422 when execution fails")
    void testPlaceOrderRejected() throws Exception {
        Account account = new Account("ACC001", "John Doe", new BigDecimal("50000.00"), AccountStatus.ACTIVE);
        Instrument instrument = new Instrument("AAPL", "Apple", new BigDecimal("150.25"), true);
        when(restTemplate.getForObject(anyString(), eq(Account.class), eq("ACC001"))).thenReturn(account);
        when(restTemplate.getForObject(anyString(), eq(Instrument.class), eq("AAPL"))).thenReturn(instrument);
        testOrder1.setOrderStatus(OrderStatus.REJECTED);
        when(orderProcessor.processOrder(account, instrument, OrderSide.BUY, new BigDecimal("100"),
                new BigDecimal("150.25"), "idempotent-key-1"))
            .thenReturn(new OrderResult(false, "BUY order ORD001 execution FAILED", null, testOrder1));

        var response = orderController.placeOrder(new PlaceOrderRequest("ACC001", "AAPL", OrderSide.BUY,
            new BigDecimal("100"), new BigDecimal("150.25"), "idempotent-key-1"));

        assertEquals(422, response.getStatusCode().value());
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertEquals("ORD-422", body.errorCode());
        assertEquals("BUY order ORD001 execution FAILED", body.message());
    }


    @Test
    @DisplayName("POST /orders returns 404 ACC-404 when the account lookup fails")
    void testPlaceOrderAccountNotFound() throws Exception {
        when(restTemplate.getForObject(anyString(), eq(Account.class), eq("ACC999")))
            .thenThrow(new org.springframework.web.client.HttpClientErrorException(org.springframework.http.HttpStatus.NOT_FOUND));

        var response = orderController.placeOrder(new PlaceOrderRequest("ACC999", "AAPL", OrderSide.BUY,
            new BigDecimal("1"), new BigDecimal("150.25"), "key-404"));

        assertEquals(404, response.getStatusCode().value());
        assertEquals("ACC-404", ((ErrorResponse) response.getBody()).errorCode());
    }

    @Test
    @DisplayName("POST /orders throws InstrumentNotFoundException when the instrument lookup fails")
    void testPlaceOrderInstrumentNotFound() {
        Account account = new Account("ACC001", "John Doe", new BigDecimal("50000.00"), AccountStatus.ACTIVE);
        when(restTemplate.getForObject(anyString(), eq(Account.class), eq("ACC001"))).thenReturn(account);
        when(restTemplate.getForObject(anyString(), eq(Instrument.class), eq("NOPE")))
            .thenThrow(new org.springframework.web.client.HttpClientErrorException(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThrows(com.neueda.orderservice.exceptions.InstrumentNotFoundException.class, () ->
            orderController.placeOrder(new PlaceOrderRequest("ACC001", "NOPE", OrderSide.BUY,
                new BigDecimal("1"), new BigDecimal("150.25"), "key-ins")));
    }
}
