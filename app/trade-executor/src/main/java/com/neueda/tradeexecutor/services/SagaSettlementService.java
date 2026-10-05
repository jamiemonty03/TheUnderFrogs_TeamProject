package com.neueda.tradeexecutor.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.neueda.tradeexecutor.clients.AccountsClient;
import com.neueda.tradeexecutor.clients.OrdersClient;
import com.neueda.tradeexecutor.clients.PositionsClient;
import com.neueda.tradeexecutor.dtos.FillDecision;
import com.neueda.tradeexecutor.dtos.OrderDto;
import com.neueda.tradeexecutor.dtos.OrderOutcomePayload;
import com.neueda.tradeexecutor.dtos.StatusUpdateResult;
import com.neueda.tradeexecutor.enums.OrderSide;
import com.neueda.tradeexecutor.enums.OrderStatus;
import com.neueda.tradeexecutor.events.TradeEventPublisher;
import com.neueda.tradeexecutor.exceptions.SettlementRejectedException;

@Service
public class SagaSettlementService implements SettlementService {

    private static final Logger log = LoggerFactory.getLogger(SagaSettlementService.class);

    private final AccountsClient accountsClient;
    private final PositionsClient positionsClient;
    private final OrdersClient ordersClient;
    private final TradeEventPublisher publisher;

    public SagaSettlementService(AccountsClient accountsClient, PositionsClient positionsClient,
            OrdersClient ordersClient, TradeEventPublisher publisher) {
        this.accountsClient = accountsClient;
        this.positionsClient = positionsClient;
        this.ordersClient = ordersClient;
        this.publisher = publisher;
    }

    @Override
    public void settle(OrderDto order, FillDecision decision) {
        log.info("Settling order {}: {} {} {} {}", order.orderId(), order.side(), order.quantity(), order.symbol(),
                decision.filled() ? "FILL @ " + decision.fillPrice() : "REJECT (" + decision.reason() + ")");

        if (!decision.filled()) {
            reverseMovements(order);
            complete(order, OrderStatus.REJECTED, null, decision.reason());
            return;
        }

        try {
            moveCashAndShares(order, decision.fillPrice());
        } catch (SettlementRejectedException e) {
            log.info("Order {} refused during settlement: {}", order.orderId(), e.getMessage());
            complete(order, OrderStatus.REJECTED, null, e.getMessage());
            return;
        }

        complete(order, OrderStatus.FILLED, decision.fillPrice(), null);
    }

    @Override
    public void compensateCancelled(OrderDto order) {
        reverseMovements(order);
        log.info("Order {} is CANCELLED; any earlier movements reversed", order.orderId());
    }

    private void moveCashAndShares(OrderDto order, BigDecimal fillPrice) {
        BigDecimal amount = fillPrice.multiply(BigDecimal.valueOf(order.quantity())).setScale(2, RoundingMode.HALF_UP);

        if (order.side() == OrderSide.BUY) {
            accountsClient.debit(order.accountId(), order.orderId(), amount);
            try {
                positionsClient.addPosition(order.accountId(), order.symbol(), order.orderId(), order.quantity(), fillPrice);
            } catch (SettlementRejectedException e) {
                accountsClient.reverse(order.accountId(), order.orderId());
                throw e;
            }
        } else {
            positionsClient.reducePosition(order.accountId(), order.symbol(), order.orderId(), order.quantity());
            try {
                accountsClient.credit(order.accountId(), order.orderId(), amount);
            } catch (SettlementRejectedException e) {
                positionsClient.reverse(order.accountId(), order.symbol(), order.orderId());
                throw e;
            }
        }
    }

    private void complete(OrderDto order, OrderStatus status, BigDecimal fillPrice, String reason) {
        StatusUpdateResult result = ordersClient.updateStatus(order.orderId(), status, reason);
        if (result.updated()) {
            publisher.publishOutcome(new OrderOutcomePayload(order.orderId(), order.accountId(), order.symbol(),
                    order.side(), order.quantity(), fillPrice, status, reason));
            return;
        }
        resolveConflict(order, result.currentStatus());
    }

    private void resolveConflict(OrderDto order, OrderStatus current) {
        switch (current) {
            case FILLED, REJECTED ->
                log.info("Order {} already {}; duplicate delivery, nothing to publish", order.orderId(), current);
            case CANCELLED -> {
                reverseMovements(order);
                log.info("Order {} was cancelled mid-settlement; movements reversed", order.orderId());
            }
            case NEW -> throw new IllegalStateException("Status update for order " + order.orderId()
                    + " conflicted but the order is still NEW");
        }
    }

    private void reverseMovements(OrderDto order) {
        positionsClient.reverse(order.accountId(), order.symbol(), order.orderId());
        accountsClient.reverse(order.accountId(), order.orderId());
    }
}
