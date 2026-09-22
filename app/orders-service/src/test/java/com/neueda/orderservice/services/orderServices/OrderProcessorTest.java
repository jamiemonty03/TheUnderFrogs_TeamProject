package com.neueda.orderservice.services.orderServices;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.neueda.orderservice.enums.AccountStatus;
import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.enums.OrderStatus;
import com.neueda.orderservice.models.Account;
import com.neueda.orderservice.models.Instrument;
import com.neueda.orderservice.models.Order;
import com.neueda.orderservice.models.Position;
import com.neueda.orderservice.repositories.InMemoryPositionRepository;
import com.neueda.orderservice.services.OrderService;

public class OrderProcessorTest {

    @Test
    void testOrderProcessorDispatchesToCorrectStrategy() throws Exception {
        OrderService orderService = mock(OrderService.class);
        BuyOrderStrategy mockBuy = mock(BuyOrderStrategy.class);
        SellOrderStrategy mockSell = mock(SellOrderStrategy.class);
        OrderProcessor processor = new OrderProcessor(orderService, mockBuy, mockSell);
        Account account = new Account("ACC-1", "Test", BigDecimal.TEN, AccountStatus.ACTIVE);
        Instrument instrument = new Instrument("AAPL", "Apple", new BigDecimal("150.00"), true);
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
        Instrument instrument = new Instrument("AAPL", "Apple", new BigDecimal("150.00"), true);
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

    @Test
    void testBuyOrderStrategyExecutionHandledCorrectly() throws Exception {
        OrderService orderService = mock(OrderService.class);
        BuyOrderStrategy mockBuy = mock(BuyOrderStrategy.class);
        SellOrderStrategy mockSell = mock(SellOrderStrategy.class);
        OrderProcessor processor = new OrderProcessor(orderService, mockBuy, mockSell);
        
        Account account = new Account("ACC-BUY", "Buyer", new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        Instrument instrument = new Instrument("AAPL", "Apple", new BigDecimal("150.00"), true);
        Order buyOrder = new Order("ORDER-3", "ACC-BUY", "AAPL", OrderSide.BUY, 2, new BigDecimal("100.00"), "buy-key");

        when(orderService.placeOrder(account, instrument, OrderSide.BUY, new BigDecimal("2"), new BigDecimal("100.00"), "buy-key"))
            .thenReturn(buyOrder);
        when(mockBuy.execute(buyOrder, account, instrument))
            .thenReturn(new OrderResult(true, "order filled successfully", null));

        OrderResult result = processor.processOrder(
            account, instrument, OrderSide.BUY,
            new BigDecimal("2"), new BigDecimal("100.00"), "buy-key"
        );

        assertTrue(result.isSuccess());
        verify(mockBuy).execute(buyOrder, account, instrument);
        verify(orderService).saveOrder(buyOrder);
    }

    @Test
    void testSellOrderStrategyExecutionHandledCorrectly() throws Exception {
        OrderService orderService = mock(OrderService.class);
        BuyOrderStrategy mockBuy = mock(BuyOrderStrategy.class);
        SellOrderStrategy mockSell = mock(SellOrderStrategy.class);
        OrderProcessor processor = new OrderProcessor(orderService, mockBuy, mockSell);
        
        Account account = new Account("ACC-SELL", "Seller", new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        Instrument instrument = new Instrument("AAPL", "Apple", new BigDecimal("150.00"), true);
        Order sellOrder = new Order("ORDER-4", "ACC-SELL", "AAPL", OrderSide.SELL, 2, new BigDecimal("100.00"), "sell-key");

        when(orderService.placeOrder(account, instrument, OrderSide.SELL, new BigDecimal("2"), new BigDecimal("100.00"), "sell-key"))
            .thenReturn(sellOrder);
        when(mockSell.execute(sellOrder, account, instrument))
            .thenReturn(new OrderResult(true, "order filled successfully", null));

        OrderResult result = processor.processOrder(
            account, instrument, OrderSide.SELL,
            new BigDecimal("2"), new BigDecimal("100.00"), "sell-key"
        );

        assertTrue(result.isSuccess());
        verify(mockSell).execute(sellOrder, account, instrument);
        verify(orderService).saveOrder(sellOrder);
    }
}
