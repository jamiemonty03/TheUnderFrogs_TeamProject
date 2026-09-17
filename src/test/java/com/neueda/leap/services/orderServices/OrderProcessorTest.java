package com.neueda.leap.services.orderServices;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.neueda.leap.enums.AccountStatus;
import com.neueda.leap.enums.OrderSide;
import com.neueda.leap.models.Account;
import com.neueda.leap.models.Instrument;
import com.neueda.leap.models.Order;
import com.neueda.leap.services.OrderService;

public class OrderProcessorTest {

    @Test
    void testOrderProcessorDispatchesToCorrectStrategy() throws Exception {
        OrderService orderService = mock(OrderService.class);
        BuyOrderStrategy mockBuy = mock(BuyOrderStrategy.class);
        SellOrderStrategy mockSell = mock(SellOrderStrategy.class);
        OrderProcessor processor = new OrderProcessor(orderService, mockBuy, mockSell);
        Account account = new Account("ACC-1", "Test", BigDecimal.TEN, AccountStatus.ACTIVE);
        Instrument instrument = new Instrument("AAPL", "Apple", "EQUITY", "USD", "NASDAQ", true);
        Order buyOrder = new Order("ORDER-1", "ACC-1", "AAPL", OrderSide.BUY, 1, BigDecimal.TEN, "key-1");

        when(orderService.placeOrder(account, instrument, OrderSide.BUY, BigDecimal.ONE, BigDecimal.TEN, "key-1"))
            .thenReturn(buyOrder);
        when(mockBuy.execute(buyOrder, account, instrument))
            .thenReturn(new OrderResult(true, "filled", null));

        processor.processOrder(account, instrument, OrderSide.BUY, BigDecimal.ONE, BigDecimal.TEN, "key-1");

        verify(mockBuy).execute(buyOrder, account, instrument);
        verify(orderService).saveOrder(buyOrder);
        verify(mockSell, never()).execute(any(), any(), any());
    }

    @Test
    void testOrderProcessorDispatchesSellAndPersistsOrder() throws Exception {
        OrderService orderService = mock(OrderService.class);
        BuyOrderStrategy mockBuy = mock(BuyOrderStrategy.class);
        SellOrderStrategy mockSell = mock(SellOrderStrategy.class);
        OrderProcessor processor = new OrderProcessor(orderService, mockBuy, mockSell);
        Account account = new Account("ACC-1", "Test", BigDecimal.TEN, AccountStatus.ACTIVE);
        Instrument instrument = new Instrument("AAPL", "Apple", "EQUITY", "USD", "NASDAQ", true);
        Order sellOrder = new Order("ORDER-2", "ACC-1", "AAPL", OrderSide.SELL, 1, BigDecimal.TEN, "key-2");

        when(orderService.placeOrder(account, instrument, OrderSide.SELL, BigDecimal.ONE, BigDecimal.TEN, "key-2"))
            .thenReturn(sellOrder);
        when(mockSell.execute(sellOrder, account, instrument))
            .thenReturn(new OrderResult(true, "filled", null));

        processor.processOrder(account, instrument, OrderSide.SELL, BigDecimal.ONE, BigDecimal.TEN, "key-2");

        verify(mockSell).execute(sellOrder, account, instrument);
        verify(orderService).saveOrder(sellOrder);
        verify(mockBuy, never()).execute(any(), any(), any());
    }
}
