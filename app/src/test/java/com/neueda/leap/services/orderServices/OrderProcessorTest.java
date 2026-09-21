package com.neueda.leap.services.orderServices;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.neueda.leap.enums.AccountStatus;
import com.neueda.leap.enums.OrderSide;
import com.neueda.leap.enums.OrderStatus;
import com.neueda.leap.models.Account;
import com.neueda.leap.models.Instrument;
import com.neueda.leap.models.Order;
import com.neueda.leap.models.Position;
import com.neueda.leap.repositories.InMemoryPositionRepository;
import com.neueda.leap.services.AccountService;
import com.neueda.leap.services.OrderService;
import com.neueda.leap.services.PositionService;

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

    @Test
    void buyOrderUpdatesAccountAndCanBeRetrievedAfterExecution() throws Exception {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        OrderService orderService = new OrderService(positionRepository);
        PositionService positionService = new PositionService(positionRepository);
        OrderProcessor processor = new OrderProcessor(
            orderService,
            new BuyOrderStrategy(new AccountService(), positionService),
            new SellOrderStrategy(new AccountService(), positionService)
        );
        Account account = new Account("ACC-BUY", "Buyer", new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        Instrument instrument = new Instrument("AAPL", "Apple", "EQUITY", "USD", "NASDAQ", true);

        OrderResult result = processor.processOrder(
            account, instrument, OrderSide.BUY,
            new BigDecimal("2"), new BigDecimal("100.00"), "buy-persistence-key"
        );

        Order savedOrder = orderService.getOrderByIdempotencyKey("buy-persistence-key").orElseThrow();
        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("800.00"), account.getCashBalance());
        assertEquals(OrderStatus.FILLED, savedOrder.getOrderStatus());
        assertEquals(2, positionService.getTotalQuantity("ACC-BUY", "AAPL"));
    }

    @Test
    void sellOrderUpdatesAccountAndCanBeRetrievedAfterExecution() throws Exception {
        InMemoryPositionRepository positionRepository = new InMemoryPositionRepository();
        positionRepository.save(new Position(
            "ACC-SELL", "AAPL", new BigDecimal("10"), new BigDecimal("90.00")
        ));
        OrderService orderService = new OrderService(positionRepository);
        PositionService positionService = new PositionService(positionRepository);
        OrderProcessor processor = new OrderProcessor(
            orderService,
            new BuyOrderStrategy(new AccountService(), positionService),
            new SellOrderStrategy(new AccountService(), positionService)
        );
        Account account = new Account("ACC-SELL", "Seller", new BigDecimal("1000.00"), AccountStatus.ACTIVE);
        Instrument instrument = new Instrument("AAPL", "Apple", "EQUITY", "USD", "NASDAQ", true);

        OrderResult result = processor.processOrder(
            account, instrument, OrderSide.SELL,
            new BigDecimal("2"), new BigDecimal("100.00"), "sell-persistence-key"
        );

        Order savedOrder = orderService.getOrderByIdempotencyKey("sell-persistence-key").orElseThrow();
        assertTrue(result.isSuccess());
        assertEquals(new BigDecimal("1200.00"), account.getCashBalance());
        assertEquals(OrderStatus.FILLED, savedOrder.getOrderStatus());
        assertEquals(8, positionService.getTotalQuantity("ACC-SELL", "AAPL"));
    }
}
