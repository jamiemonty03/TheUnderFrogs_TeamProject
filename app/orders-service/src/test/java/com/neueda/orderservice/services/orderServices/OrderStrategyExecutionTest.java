package com.neueda.orderservice.services.orderServices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

import com.neueda.orderservice.enums.AccountStatus;
import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.enums.OrderStatus;
import com.neueda.orderservice.models.Account;
import com.neueda.orderservice.models.Instrument;
import com.neueda.orderservice.models.Order;
import com.neueda.orderservice.clients.AccountsClient;

/**
 * Runs the real BUY/SELL strategies against a mocked AccountsClient.
 */
class OrderStrategyExecutionTest {

    private AccountsClient accountsClient;
    private Account account;
    private Instrument instrument;

    @BeforeEach
    void setUp() {
        accountsClient = mock(AccountsClient.class);
        account = new Account("ACC-1", "Test", new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        instrument = new Instrument("AAPL", "Apple", new BigDecimal("150.00"), true);
    }

    @Test
    @DisplayName("BUY debits quantity * price and marks the order FILLED")
    void buyDebitsAndFills() {
        Order order = new Order("ORDER-1", "ACC-1", "AAPL", OrderSide.BUY, 5, new BigDecimal("100.00"), "key-1");

        OrderResult result = new BuyOrderStrategy(accountsClient).execute(order, account, instrument);

        assertTrue(result.isSuccess());
        assertSame(order, result.getOrder());
        assertEquals(OrderStatus.FILLED, order.getOrderStatus());
        verify(accountsClient).debit(account, new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("BUY is REJECTED when the debit fails, and nothing is credited back")
    void buyRejectedWhenDebitFails() {
        Order order = new Order("ORDER-2", "ACC-1", "AAPL", OrderSide.BUY, 5, new BigDecimal("100.00"), "key-2");
        doThrow(new RestClientException("400 Insufficient funds"))
            .when(accountsClient).debit(account, new BigDecimal("500.00"));

        OrderResult result = new BuyOrderStrategy(accountsClient).execute(order, account, instrument);

        assertFalse(result.isSuccess());
        assertEquals(OrderStatus.REJECTED, order.getOrderStatus());
        verify(accountsClient, never()).credit(account, new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("SELL credits quantity * price and marks the order FILLED")
    void sellCreditsAndFills() {
        Order order = new Order("ORDER-3", "ACC-1", "AAPL", OrderSide.SELL, 2, new BigDecimal("150.00"), "key-3");

        OrderResult result = new SellOrderStrategy(accountsClient).execute(order, account, instrument);

        assertTrue(result.isSuccess());
        assertSame(order, result.getOrder());
        assertEquals(OrderStatus.FILLED, order.getOrderStatus());
        verify(accountsClient).credit(account, new BigDecimal("300.00"));
    }

    @Test
    @DisplayName("SELL is REJECTED when the credit fails")
    void sellRejectedWhenCreditFails() {
        Order order = new Order("ORDER-4", "ACC-1", "AAPL", OrderSide.SELL, 2, new BigDecimal("150.00"), "key-4");
        doThrow(new RestClientException("accounts-service unavailable"))
            .when(accountsClient).credit(account, new BigDecimal("300.00"));

        OrderResult result = new SellOrderStrategy(accountsClient).execute(order, account, instrument);

        assertFalse(result.isSuccess());
        assertEquals(OrderStatus.REJECTED, order.getOrderStatus());
    }

    @Test
    @DisplayName("BUY credits the cash back when a step after the debit fails")
    void buyRollsBackDebitOnLaterFailure() {
        Order order = orderFailingOnFill(OrderSide.BUY);

        OrderResult result = new BuyOrderStrategy(accountsClient).execute(order, account, instrument);

        assertFalse(result.isSuccess());
        assertEquals(OrderStatus.REJECTED, order.getOrderStatus());
        verify(accountsClient).debit(account, new BigDecimal("500.00"));
        verify(accountsClient).credit(account, new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("BUY is still REJECTED when the rollback credit also fails")
    void buyRejectedWhenRollbackFails() {
        Order order = orderFailingOnFill(OrderSide.BUY);
        doThrow(new RestClientException("accounts-service unavailable"))
            .when(accountsClient).credit(account, new BigDecimal("500.00"));

        OrderResult result = new BuyOrderStrategy(accountsClient).execute(order, account, instrument);

        assertFalse(result.isSuccess());
        assertEquals(OrderStatus.REJECTED, order.getOrderStatus());
    }

    @Test
    @DisplayName("SELL debits the proceeds back when a step after the credit fails")
    void sellRollsBackCreditOnLaterFailure() {
        Order order = orderFailingOnFill(OrderSide.SELL);

        OrderResult result = new SellOrderStrategy(accountsClient).execute(order, account, instrument);

        assertFalse(result.isSuccess());
        assertEquals(OrderStatus.REJECTED, order.getOrderStatus());
        verify(accountsClient).credit(account, new BigDecimal("500.00"));
        verify(accountsClient).debit(account, new BigDecimal("500.00"));
    }

    @Test
    @DisplayName("SELL is still REJECTED when the rollback debit also fails")
    void sellRejectedWhenRollbackFails() {
        Order order = orderFailingOnFill(OrderSide.SELL);
        doThrow(new RestClientException("accounts-service unavailable"))
            .when(accountsClient).debit(account, new BigDecimal("500.00"));

        OrderResult result = new SellOrderStrategy(accountsClient).execute(order, account, instrument);

        assertFalse(result.isSuccess());
        assertEquals(OrderStatus.REJECTED, order.getOrderStatus());
    }

    /** An order whose first status change (to FILLED) throws, simulating a failure after the cash moved. */
    private Order orderFailingOnFill(OrderSide side) {
        Order order = spy(new Order("ORDER-RB", "ACC-1", "AAPL", side, 5, new BigDecimal("100.00"), "key-rb"));
        doThrow(new IllegalStateException("simulated failure")).doCallRealMethod()
            .when(order).setOrderStatus(any());
        return order;
    }
}
