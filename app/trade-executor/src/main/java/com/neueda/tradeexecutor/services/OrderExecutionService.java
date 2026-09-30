package com.neueda.tradeexecutor.services;

import java.math.BigDecimal;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.neueda.tradeexecutor.clients.InstrumentsClient;
import com.neueda.tradeexecutor.clients.OrdersClient;
import com.neueda.tradeexecutor.dtos.FillDecision;
import com.neueda.tradeexecutor.dtos.InstrumentDto;
import com.neueda.tradeexecutor.dtos.OrderDto;
import com.neueda.tradeexecutor.enums.OrderStatus;

@Service
public class OrderExecutionService {

    private static final Logger log = LoggerFactory.getLogger(OrderExecutionService.class);

    private final OrdersClient ordersClient;
    private final InstrumentsClient instrumentsClient;
    private final PriceSource priceSource;
    private final SettlementService settlementService;

    public OrderExecutionService(OrdersClient ordersClient, InstrumentsClient instrumentsClient,
            PriceSource priceSource, SettlementService settlementService) {
        this.ordersClient = ordersClient;
        this.instrumentsClient = instrumentsClient;
        this.priceSource = priceSource;
        this.settlementService = settlementService;
    }

    public void execute(String orderId) {
        OrderDto order = ordersClient.getOrder(orderId);

        if (order.orderStatus() != OrderStatus.NEW) {
            log.info("Skipping order {}: status is {}", orderId, order.orderStatus());
            return;
        }

        FillDecision decision = decide(order);
        settlementService.settle(order, decision);
    }

    private FillDecision decide(OrderDto order) {
        Optional<InstrumentDto> instrument = instrumentsClient.getInstrument(order.symbol());

        if (instrument.isEmpty()) {
            return FillDecision.reject("Unknown symbol " + order.symbol());
        }
        if (!instrument.get().tradable()) {
            return FillDecision.reject("Instrument " + order.symbol() + " is not tradable");
        }

        BigDecimal marketPrice = priceSource.getPrice(instrument.get(), order.side()).orElse(null);
        return FillRule.decide(order, marketPrice);
    }
}
