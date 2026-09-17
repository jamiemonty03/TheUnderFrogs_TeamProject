package com.neueda.leap.services.orderServices;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.neueda.leap.enums.AccountStatus;
import com.neueda.leap.enums.OrderSide;
import com.neueda.leap.enums.OrderStatus;
import com.neueda.leap.exceptions.InsufficientHoldingsException;
import com.neueda.leap.models.Account;
import com.neueda.leap.models.Instrument;
import com.neueda.leap.models.Order;
import com.neueda.leap.services.AccountService;
import com.neueda.leap.services.PositionManager;

class SellOrderStrategyTest {

    private Account account;
    private Instrument instrument;
    private PositionManager positionManager;
    private SellOrderStrategy strategy;

    @BeforeEach
    void setUp() {
        account = new Account("ACC-1", "Test", new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        instrument = new Instrument("AAPL", "Apple", "EQUITY", "USD", "NASDAQ", true);
        positionManager = mock(PositionManager.class);
        strategy = new SellOrderStrategy(new AccountService(), positionManager);
    }

    @Test
    void fillsOrderCreditsCashAndUpdatesPosition() throws InsufficientHoldingsException {
        Order order = new Order("ORDER-1", "ACC-1", "AAPL", OrderSide.SELL, 5,
            new BigDecimal("100.00"), "key-1");

        OrderResult result = strategy.execute(order, account, instrument);

        assertTrue(result.isSuccess());
        assertEquals(OrderStatus.FILLED, order.getOrderStatus());
        assertEquals(new BigDecimal("1500.00"), account.getCashBalance());
        verify(positionManager).updatePositionAfterSell("ACC-1", "AAPL", 5);
    }

    @Test
    void rejectsOrderAndRestoresCashWhenPositionUpdateFails() throws InsufficientHoldingsException {
        Order order = new Order("ORDER-2", "ACC-1", "AAPL", OrderSide.SELL, 5,
            new BigDecimal("100.00"), "key-2");
        doThrow(new IllegalStateException("position store unavailable"))
            .when(positionManager).updatePositionAfterSell("ACC-1", "AAPL", 5);

        OrderResult result = strategy.execute(order, account, instrument);

        assertFalse(result.isSuccess());
        assertEquals(OrderStatus.REJECTED, order.getOrderStatus());
        assertEquals(new BigDecimal("1000.00"), account.getCashBalance());
    }
}