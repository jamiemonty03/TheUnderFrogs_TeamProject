package com.neueda.tradeexecutor.clients;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.neueda.tradeexecutor.dtos.OrderDto;
import com.neueda.tradeexecutor.exceptions.UnknownOrderException;
import com.neueda.tradeexecutor.enums.OrderSide;
import com.neueda.tradeexecutor.enums.OrderStatus;

class OrdersClientTest {

    private static final String ORDERS_URL = "http://orders-service/api/orders";

    private MockRestServiceServer ordersService;
    private OrdersClient ordersClient;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        ordersService = MockRestServiceServer.bindTo(restTemplate).build();
        ordersClient = new OrdersClient(restTemplate, ORDERS_URL);
    }

    private static String orderJson(String status) {
        return """
            {
              "orderId": "ORD-1",
              "accountId": "ACC-1",
              "symbol": "AAPL",
              "side": "BUY",
              "quantity": 10,
              "price": 150.00,
              "orderStatus": "%s",
              "version": 0,
              "createdAt": "2026-09-28T10:00:00",
              "lastUpdated": "2026-09-28T10:00:00",
              "updatedBy": "SYSTEM"
            }
            """.formatted(status);
    }

    @Test
    void getOrderReturnsTheOrder() {
        ordersService.expect(requestTo(ORDERS_URL + "/ORD-1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(orderJson("NEW"), MediaType.APPLICATION_JSON));

        OrderDto order = ordersClient.getOrder("ORD-1");

        assertEquals("ORD-1", order.orderId());
        assertEquals("ACC-1", order.accountId());
        assertEquals("AAPL", order.symbol());
        assertEquals(OrderSide.BUY, order.side());
        assertEquals(10, order.quantity());
        assertEquals(0, new BigDecimal("150.00").compareTo(order.price()));
        assertEquals(OrderStatus.NEW, order.orderStatus());
        ordersService.verify();
    }

    @Test
    void getOrderReturnsCurrentStatusWhenNoLongerNew() {
        ordersService.expect(requestTo(ORDERS_URL + "/ORD-1"))
                .andRespond(withSuccess(orderJson("CANCELLED"), MediaType.APPLICATION_JSON));

        OrderDto order = ordersClient.getOrder("ORD-1");

        assertEquals(OrderStatus.CANCELLED, order.orderStatus());
    }

    @Test
    void getOrderThrowsWhenOrderDoesNotExist() {
        ordersService.expect(requestTo(ORDERS_URL + "/MISSING"))
                .andRespond(withResourceNotFound());

        assertThrows(UnknownOrderException.class, () -> ordersClient.getOrder("MISSING"));
    }
}
