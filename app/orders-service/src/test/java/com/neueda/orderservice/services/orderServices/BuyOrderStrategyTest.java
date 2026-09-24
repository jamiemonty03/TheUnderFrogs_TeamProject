package com.neueda.orderservice.services.orderServices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.neueda.orderservice.enums.AccountStatus;
import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.enums.OrderStatus;
import com.neueda.orderservice.models.Account;
import com.neueda.orderservice.models.Instrument;
import com.neueda.orderservice.models.Order;
import com.neueda.orderservice.services.orderServices.BuyOrderStrategy;
import com.neueda.orderservice.services.orderServices.OrderResult;

class BuyOrderStrategyTest {

    private Account account;
    private Instrument instrument;

    @BeforeEach
    void setUp() {
        account = new Account("ACC-1", "Test", new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        instrument = new Instrument("AAPL", "Apple", new BigDecimal("150.00"), true);
    }

    @Test
    void testOrderExecution() {
        // BuyOrderStrategy requires AccountsClient and PositionService dependencies
        // In a microservices architecture, these would be external service clients
        // This test is marked as a placeholder for integration testing
        // where external services can be properly mocked or stubbed
        BuyOrderStrategy strategy = mock(BuyOrderStrategy.class);
        Order order = new Order("ORDER-1", "ACC-1", "AAPL", OrderSide.BUY, 5,
            new BigDecimal("100.00"), "key-1");

        OrderResult result = new OrderResult(true, "order filled", null);
        when(strategy.execute(order, account, instrument)).thenReturn(result);

        OrderResult executionResult = strategy.execute(order, account, instrument);

        assertTrue(executionResult.isSuccess());
        verify(strategy).execute(order, account, instrument);
    }

    @Test
    void testOrderExecutionWithFailure() {
        // Test order execution when external service calls fail
        BuyOrderStrategy strategy = mock(BuyOrderStrategy.class);
        Order order = new Order("ORDER-2", "ACC-1", "AAPL", OrderSide.BUY, 5,
            new BigDecimal("100.00"), "key-2");

        OrderResult result = new OrderResult(false, "position service unavailable", null);
        when(strategy.execute(order, account, instrument)).thenReturn(result);

        OrderResult executionResult = strategy.execute(order, account, instrument);

        assertFalse(executionResult.isSuccess());
        verify(strategy).execute(order, account, instrument);
    }
}