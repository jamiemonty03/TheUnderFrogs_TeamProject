package com.neueda.orderservice.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import com.neueda.orderservice.config.SecurityConfig;
import com.neueda.orderservice.enums.AccountStatus;
import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.enums.OrderStatus;
import com.neueda.orderservice.exceptions.OrderNotCancellableException;
import com.neueda.orderservice.exceptions.OrderNotFoundException;
import com.neueda.orderservice.models.Account;
import com.neueda.orderservice.models.Instrument;
import com.neueda.orderservice.models.Order;
import com.neueda.orderservice.repositories.OrderRepository;
import com.neueda.orderservice.services.OrderCancellationService;
import com.neueda.orderservice.services.OrderStatusService;
import com.neueda.orderservice.services.orderServices.OrderProcessor;
import com.neueda.orderservice.services.orderServices.OrderResult;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
class OrderControllerMvcIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderProcessor orderProcessor;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private OrderCancellationService orderCancellationService;

    @MockBean
    private OrderStatusService orderStatusService;

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order(
                "order-1",
                "ACC0001",
                "AAPL",
                OrderSide.BUY,
                10,
                new BigDecimal("150.00"),
                "idempotency-key-1");
        order.setOrderStatus(OrderStatus.FILLED);
    }

    @Test
    void getAllOrdersReturnsJsonThroughHttpEndpoint() throws Exception {
        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(order));

        mockMvc.perform(get("/orders").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value("order-1"))
                .andExpect(jsonPath("$[0].accountId").value("ACC0001"))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].orderStatus").value("FILLED"));
    }

    @Test
    void getOrderByIdReturnsNotFoundWhenOrderDoesNotExist() throws Exception {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/orders/missing").with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrdersByAccountReturnsEmptyArrayWhenNoOrdersExist() throws Exception {
        when(orderRepository.findByAccountIdOrderByCreatedAtDesc("ACC9999")).thenReturn(List.of());

        mockMvc.perform(get("/orders/account/ACC9999").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void placeOrderReturnsCreatedResponse() throws Exception {
        Account account = new Account("ACC0001", "Alice Johnson", new BigDecimal("10000.00"), AccountStatus.ACTIVE);
        Instrument instrument = new Instrument("AAPL", "Apple Inc.", new BigDecimal("150.00"), true);
        when(restTemplate.getForObject(anyString(), eq(Account.class), eq("ACC0001"))).thenReturn(account);
        when(restTemplate.getForObject(anyString(), eq(Instrument.class), eq("AAPL"))).thenReturn(instrument);
        when(orderProcessor.processOrder(
                eq(account),
                eq(instrument),
                eq(OrderSide.BUY),
                eq(new BigDecimal("10")),
                eq(new BigDecimal("150.00")),
                eq("idempotency-key-2")))
                .thenReturn(new OrderResult(true, "Order filled", null, order));

        mockMvc.perform(post("/orders")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": "ACC0001",
                                  "symbol": "AAPL",
                                  "side": "BUY",
                                  "quantity": 10,
                                  "priceLimit": 150.00,
                                  "idempotencyKey": "idempotency-key-2"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("order-1"))
                .andExpect(jsonPath("$.orderStatus").value("FILLED"));
    }

    @Test
    void placeOrderRejectsInvalidRequestBody() throws Exception {
        mockMvc.perform(post("/orders")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void updateOrderReturnsUpdatedOrder() throws Exception {
        when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

        mockMvc.perform(put("/orders/order-1")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 20, "updatedBy": "integration-test"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order-1"))
                .andExpect(jsonPath("$.quantity").value(20))
                .andExpect(jsonPath("$.updatedBy").value("integration-test"));

        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void deleteOrderReturnsNoContentWhenOrderIsCancelled() throws Exception {
        mockMvc.perform(delete("/orders/order-1").with(jwt()))
                .andExpect(status().isNoContent());

        verify(orderCancellationService).cancel("order-1");
    }

    @Test
    void deleteOrderReturnsConflictWithCurrentStatusWhenNoLongerNew() throws Exception {
        doThrow(new OrderNotCancellableException("order-1", OrderStatus.FILLED))
                .when(orderCancellationService).cancel("order-1");

        mockMvc.perform(delete("/orders/order-1").with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ORD-409"))
                .andExpect(jsonPath("$.message").value("Order order-1 cannot be cancelled: status is FILLED"));
    }

    @Test
    void deleteOrderReturnsNotFoundWhenOrderDoesNotExist() throws Exception {
        doThrow(new OrderNotFoundException("missing"))
                .when(orderCancellationService).cancel("missing");

        mockMvc.perform(delete("/orders/missing").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ORD-404"));
    }
}
