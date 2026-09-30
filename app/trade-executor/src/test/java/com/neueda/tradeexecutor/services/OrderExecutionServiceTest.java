package com.neueda.tradeexecutor.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.http.HttpStatus;
import com.neueda.tradeexecutor.clients.InstrumentsClient;
import com.neueda.tradeexecutor.clients.OrdersClient;
import com.neueda.tradeexecutor.dtos.FillDecision;
import com.neueda.tradeexecutor.dtos.InstrumentDto;
import com.neueda.tradeexecutor.dtos.OrderDto;
import com.neueda.tradeexecutor.enums.OrderSide;
import com.neueda.tradeexecutor.enums.OrderStatus;

@ExtendWith(MockitoExtension.class)
class OrderExecutionServiceTest {

    @Mock private OrdersClient ordersClient;
    @Mock private InstrumentsClient instrumentsClient;
    @Mock private PriceSource priceSource;
    @Mock private SettlementService settlementService;

    private OrderExecutionService executionService;

    private static final InstrumentDto AAPL = new InstrumentDto("AAPL", "Equity", true);

    @BeforeEach
    void setUp() {
        executionService = new OrderExecutionService(ordersClient, instrumentsClient, priceSource, settlementService);
    }

    private static OrderDto buyOrder(OrderStatus status) {
        return new OrderDto("ORD-1", "ACC-1", "AAPL", OrderSide.BUY, 10, new BigDecimal("150.00"), status);
    }

    @Test
    void skipsOrderThatIsNoLongerNew() {
        when(ordersClient.getOrder("ORD-1")).thenReturn(buyOrder(OrderStatus.CANCELLED));

        executionService.execute("ORD-1");

        verifyNoInteractions(instrumentsClient, priceSource, settlementService);
    }

    @Test
    void rejectsUnknownSymbol() {
        OrderDto order = buyOrder(OrderStatus.NEW);
        when(ordersClient.getOrder("ORD-1")).thenReturn(order);
        when(instrumentsClient.getInstrument("AAPL")).thenReturn(Optional.empty());

        executionService.execute("ORD-1");

        verify(settlementService).settle(order, FillDecision.reject("Unknown symbol AAPL"));
        verifyNoInteractions(priceSource);
    }

    @Test
    void rejectsNonTradableInstrument() {
        OrderDto order = buyOrder(OrderStatus.NEW);
        when(ordersClient.getOrder("ORD-1")).thenReturn(order);
        when(instrumentsClient.getInstrument("AAPL"))
                .thenReturn(Optional.of(new InstrumentDto("AAPL", "Equity", false)));

        executionService.execute("ORD-1");

        verify(settlementService).settle(order, FillDecision.reject("Instrument AAPL is not tradable"));
        verifyNoInteractions(priceSource);
    }

    @Test
    void rejectsWhenNoPriceIsAvailable() {
        OrderDto order = buyOrder(OrderStatus.NEW);
        when(ordersClient.getOrder("ORD-1")).thenReturn(order);
        when(instrumentsClient.getInstrument("AAPL")).thenReturn(Optional.of(AAPL));
        when(priceSource.getPrice(AAPL)).thenReturn(Optional.empty());

        executionService.execute("ORD-1");

        verify(settlementService).settle(order, FillDecision.reject("No valid market price for AAPL"));
    }

    @Test
    void fillsAtMarketPriceWhenWithinLimit() {
        OrderDto order = buyOrder(OrderStatus.NEW);
        when(ordersClient.getOrder("ORD-1")).thenReturn(order);
        when(instrumentsClient.getInstrument("AAPL")).thenReturn(Optional.of(AAPL));
        when(priceSource.getPrice(AAPL)).thenReturn(Optional.of(new BigDecimal("148.50")));

        executionService.execute("ORD-1");

        verify(settlementService).settle(order, FillDecision.fill(new BigDecimal("148.50")));
    }

    @Test
    void rejectsWhenMarketPriceIsBeyondLimit() {
        OrderDto order = buyOrder(OrderStatus.NEW);
        when(ordersClient.getOrder("ORD-1")).thenReturn(order);
        when(instrumentsClient.getInstrument("AAPL")).thenReturn(Optional.of(AAPL));
        when(priceSource.getPrice(AAPL)).thenReturn(Optional.of(new BigDecimal("151.00")));

        executionService.execute("ORD-1");

        verify(settlementService).settle(order,
                FillDecision.reject("Market price 151.00 is above BUY limit 150.00"));
    }

    @Test
    void doesNotSettleWhenADownstreamServiceFails() {
        when(ordersClient.getOrder("ORD-1")).thenReturn(buyOrder(OrderStatus.NEW));
        when(instrumentsClient.getInstrument("AAPL"))
                .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(HttpServerErrorException.class, () -> executionService.execute("ORD-1"));

        verify(settlementService, never()).settle(any(), any());
    }
}
