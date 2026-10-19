package com.neueda.tradeexecutor.services;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;
import com.neueda.tradeexecutor.clients.OrdersClient;
import com.neueda.tradeexecutor.dtos.OrderDto;
import com.neueda.tradeexecutor.enums.OrderSide;
import com.neueda.tradeexecutor.enums.OrderStatus;
import com.neueda.tradeexecutor.events.PriceChanged;

@ExtendWith(MockitoExtension.class)
class RestingOrderMatcherTest {

    @Mock private OrdersClient ordersClient;
    @Mock private OrderExecutionService executionService;

    private RestingOrderMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new RestingOrderMatcher(ordersClient, executionService);
    }

    private static OrderDto newOrder(String orderId) {
        return new OrderDto(orderId, "ACC-1", "AAPL", OrderSide.BUY, 10, new BigDecimal("150.00"), OrderStatus.NEW);
    }

    @Test
    void rechecksEveryNewOrderForTheSymbol() {
        when(ordersClient.getOpenOrders("AAPL")).thenReturn(List.of(newOrder("ORD-1"), newOrder("ORD-2")));

        matcher.onPriceChanged(new PriceChanged("AAPL"));

        verify(executionService).execute("ORD-1");
        verify(executionService).execute("ORD-2");
    }

    @Test
    void oneFailingOrderDoesNotStopTheRest() {
        when(ordersClient.getOpenOrders("AAPL")).thenReturn(List.of(newOrder("ORD-1"), newOrder("ORD-2")));
        doThrow(new ResourceAccessException("accounts-service unreachable")).when(executionService).execute("ORD-1");

        matcher.onPriceChanged(new PriceChanged("AAPL"));

        verify(executionService).execute("ORD-2");
    }

    @Test
    void ordersServiceBeingDownIsSwallowedUntilTheNextPrice() {
        when(ordersClient.getOpenOrders("AAPL")).thenThrow(new ResourceAccessException("orders-service unreachable"));

        matcher.onPriceChanged(new PriceChanged("AAPL"));

        verifyNoInteractions(executionService);
    }
}
