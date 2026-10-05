package com.neueda.orderservice.services.orderServices;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import com.neueda.orderservice.events.OrderPlacedApplicationEvent;
import com.neueda.orderservice.enums.AccountStatus;
import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.models.Account;
import com.neueda.orderservice.models.Instrument;
import com.neueda.orderservice.models.Order;
import com.neueda.orderservice.services.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class OrderProcessorTest {

    @Test
    void savesOrderAndRegistersPlacementEventWithoutExecutingIt() throws Exception {
        OrderService orderService = mock(OrderService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        OrderProcessor processor = new OrderProcessor(orderService, eventPublisher);
        Account account = new Account("ACC-1", "Test", BigDecimal.TEN, AccountStatus.ACTIVE);
        Instrument instrument = new Instrument("AAPL", "Apple", new BigDecimal("150.00"), true);
        Order order = new Order("ORDER-1", "ACC-1", "AAPL", OrderSide.BUY, 1, BigDecimal.TEN, "key-1");
        org.mockito.Mockito.when(orderService.placeOrder(
                account, instrument, OrderSide.BUY, BigDecimal.ONE, BigDecimal.TEN, "key-1"))
                .thenReturn(order);

        OrderResult result = processor.processOrder(
                account, instrument, OrderSide.BUY, BigDecimal.ONE, BigDecimal.TEN, "key-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOrder()).isSameAs(order);
        assertThat(order.getOrderStatus().name()).isEqualTo("NEW");
        verify(orderService).placeOrder(account, instrument, OrderSide.BUY, BigDecimal.ONE, BigDecimal.TEN, "key-1");
        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(OrderPlacedApplicationEvent.class));
    }
}
