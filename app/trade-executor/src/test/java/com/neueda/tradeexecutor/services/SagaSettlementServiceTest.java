package com.neueda.tradeexecutor.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
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
import com.neueda.tradeexecutor.exceptions.TradeEventPublishException;

@ExtendWith(MockitoExtension.class)
class SagaSettlementServiceTest {

    @Mock private AccountsClient accountsClient;
    @Mock private PositionsClient positionsClient;
    @Mock private OrdersClient ordersClient;
    @Mock private TradeEventPublisher publisher;

    private SagaSettlementService saga;

    private static final BigDecimal PRICE = new BigDecimal("150.00");
    private static final BigDecimal AMOUNT = new BigDecimal("1500.00");
    private static final FillDecision FILL = FillDecision.fill(PRICE);

    @BeforeEach
    void setUp() {
        saga = new SagaSettlementService(accountsClient, positionsClient, ordersClient, publisher);
    }

    private static OrderDto order(OrderSide side) {
        return new OrderDto("ORD-1", "ACC-1", "AAPL", side, 10, PRICE, OrderStatus.NEW);
    }

    private void statusUpdates(OrderStatus newStatus) {
        when(ordersClient.updateStatus(eq("ORD-1"), eq(newStatus), any()))
                .thenReturn(new StatusUpdateResult(true, newStatus));
    }

    private void statusConflicts(OrderStatus newStatus, OrderStatus current) {
        when(ordersClient.updateStatus(eq("ORD-1"), eq(newStatus), any()))
                .thenReturn(new StatusUpdateResult(false, current));
    }

    private OrderOutcomePayload publishedOutcome() {
        ArgumentCaptor<OrderOutcomePayload> captor = ArgumentCaptor.forClass(OrderOutcomePayload.class);
        verify(publisher).publishOutcome(captor.capture());
        return captor.getValue();
    }

    private static SettlementRejectedException refused(String reason) {
        return new SettlementRejectedException(reason, null);
    }

    private static HttpServerErrorException unavailable() {
        return new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
    }

    // Fills

    @Test
    void buyDebitsCashThenAddsPositionThenFillsThenPublishes() {
        statusUpdates(OrderStatus.FILLED);

        saga.settle(order(OrderSide.BUY), FILL);

        InOrder steps = inOrder(accountsClient, positionsClient, ordersClient, publisher);
        steps.verify(accountsClient).debit("ACC-1", "ORD-1", AMOUNT);
        steps.verify(positionsClient).addPosition("ACC-1", "AAPL", "ORD-1", 10, PRICE);
        steps.verify(ordersClient).updateStatus("ORD-1", OrderStatus.FILLED, null);
        steps.verify(publisher).publishOutcome(any());

        OrderOutcomePayload outcome = publishedOutcome();
        assertEquals(OrderStatus.FILLED, outcome.status());
        assertEquals(PRICE, outcome.fillPrice());
        assertEquals("ACC-1", outcome.accountId());
        assertNull(outcome.reason());
    }

    @Test
    void sellReducesPositionThenCreditsCashThenFillsThenPublishes() {
        statusUpdates(OrderStatus.FILLED);

        saga.settle(order(OrderSide.SELL), FILL);

        InOrder steps = inOrder(accountsClient, positionsClient, ordersClient, publisher);
        steps.verify(positionsClient).reducePosition("ACC-1", "AAPL", "ORD-1", 10);
        steps.verify(accountsClient).credit("ACC-1", "ORD-1", AMOUNT);
        steps.verify(ordersClient).updateStatus("ORD-1", OrderStatus.FILLED, null);
        steps.verify(publisher).publishOutcome(any());
    }

    @Test
    void amountIsRoundedToCents() {
        statusUpdates(OrderStatus.FILLED);

        saga.settle(order(OrderSide.BUY), FillDecision.fill(new BigDecimal("0.3333")));

        verify(accountsClient).debit("ACC-1", "ORD-1", new BigDecimal("3.33"));
    }

    // Rejects

    @Test
    void rejectDecisionMovesNothingAndPublishesRejected() {
        statusUpdates(OrderStatus.REJECTED);

        saga.settle(order(OrderSide.BUY), FillDecision.reject("Instrument AAPL is not tradable"));

        verifyNoInteractions(accountsClient, positionsClient);
        verify(ordersClient).updateStatus("ORD-1", OrderStatus.REJECTED, "Instrument AAPL is not tradable");
        OrderOutcomePayload outcome = publishedOutcome();
        assertEquals(OrderStatus.REJECTED, outcome.status());
        assertNull(outcome.fillPrice());
        assertEquals("Instrument AAPL is not tradable", outcome.reason());
    }

    @Test
    void insufficientFundsOnFirstStepRejectsWithoutMovingShares() {
        doThrow(refused("Insufficient funds")).when(accountsClient).debit(anyString(), anyString(), any());
        statusUpdates(OrderStatus.REJECTED);

        saga.settle(order(OrderSide.BUY), FILL);

        verifyNoInteractions(positionsClient);
        verify(accountsClient, never()).reverse(anyString(), anyString());
        verify(ordersClient).updateStatus("ORD-1", OrderStatus.REJECTED, "Insufficient funds");
        assertEquals(OrderStatus.REJECTED, publishedOutcome().status());
    }

    @Test
    void insufficientHoldingsOnFirstStepRejectsWithoutMovingCash() {
        doThrow(refused("Insufficient holdings")).when(positionsClient)
                .reducePosition(anyString(), anyString(), anyString(), anyInt());
        statusUpdates(OrderStatus.REJECTED);

        saga.settle(order(OrderSide.SELL), FILL);

        verifyNoInteractions(accountsClient);
        verify(positionsClient, never()).reverse(anyString(), anyString(), anyString());
        verify(ordersClient).updateStatus("ORD-1", OrderStatus.REJECTED, "Insufficient holdings");
    }

    @Test
    void buyPositionStepRefusedReversesCashThenRejects() {
        doThrow(refused("Position refused")).when(positionsClient)
                .addPosition(anyString(), anyString(), anyString(), anyInt(), any());
        statusUpdates(OrderStatus.REJECTED);

        saga.settle(order(OrderSide.BUY), FILL);

        InOrder steps = inOrder(accountsClient, ordersClient, publisher);
        steps.verify(accountsClient).debit("ACC-1", "ORD-1", AMOUNT);
        steps.verify(accountsClient).reverse("ACC-1", "ORD-1");
        steps.verify(ordersClient).updateStatus("ORD-1", OrderStatus.REJECTED, "Position refused");
        steps.verify(publisher).publishOutcome(any());
    }

    @Test
    void sellCashStepRefusedReversesPositionThenRejects() {
        doThrow(refused("Account not active")).when(accountsClient).credit(anyString(), anyString(), any());
        statusUpdates(OrderStatus.REJECTED);

        saga.settle(order(OrderSide.SELL), FILL);

        InOrder steps = inOrder(positionsClient, ordersClient);
        steps.verify(positionsClient).reducePosition("ACC-1", "AAPL", "ORD-1", 10);
        steps.verify(positionsClient).reverse("ACC-1", "AAPL", "ORD-1");
        steps.verify(ordersClient).updateStatus("ORD-1", OrderStatus.REJECTED, "Account not active");
    }

    @Test
    void failedReversalPropagatesAndLeavesTheOrderNew() {
        doThrow(refused("Position refused")).when(positionsClient)
                .addPosition(anyString(), anyString(), anyString(), anyInt(), any());
        doThrow(unavailable()).when(accountsClient).reverse(anyString(), anyString());

        assertThrows(HttpServerErrorException.class, () -> saga.settle(order(OrderSide.BUY), FILL));

        verify(ordersClient, never()).updateStatus(anyString(), any(), any());
        verifyNoInteractions(publisher);
    }

    // 409 on the status change

    @Test
    void conflictWithFilledIsADuplicateAndPublishesNothing() {
        statusConflicts(OrderStatus.FILLED, OrderStatus.FILLED);

        saga.settle(order(OrderSide.BUY), FILL);

        verify(accountsClient, never()).reverse(anyString(), anyString());
        verify(positionsClient, never()).reverse(anyString(), anyString(), anyString());
        verifyNoInteractions(publisher);
    }

    @Test
    void conflictWithRejectedIsADuplicateAndPublishesNothing() {
        statusConflicts(OrderStatus.REJECTED, OrderStatus.REJECTED);

        saga.settle(order(OrderSide.BUY), FillDecision.reject("Unknown symbol AAPL"));

        verifyNoInteractions(publisher);
    }

    @Test
    void buyCancelledMidSettlementReversesBothMovementsAndPublishesNothing() {
        statusConflicts(OrderStatus.FILLED, OrderStatus.CANCELLED);

        saga.settle(order(OrderSide.BUY), FILL);

        InOrder steps = inOrder(positionsClient, accountsClient);
        steps.verify(positionsClient).reverse("ACC-1", "AAPL", "ORD-1");
        steps.verify(accountsClient).reverse("ACC-1", "ORD-1");
        verifyNoInteractions(publisher);
    }

    @Test
    void conflictButStillNewThrowsSoKafkaRetries() {
        statusConflicts(OrderStatus.FILLED, OrderStatus.NEW);

        assertThrows(IllegalStateException.class, () -> saga.settle(order(OrderSide.BUY), FILL));
        verifyNoInteractions(publisher);
    }

    // Transient failures and redelivery

    @Test
    void transientErrorOnSecondStepPropagatesWithoutReversingOrPublishing() {
        doThrow(unavailable()).when(positionsClient)
                .addPosition(anyString(), anyString(), anyString(), anyInt(), any());

        assertThrows(HttpServerErrorException.class, () -> saga.settle(order(OrderSide.BUY), FILL));

        verify(accountsClient, never()).reverse(anyString(), anyString());
        verify(ordersClient, never()).updateStatus(anyString(), any(), any());
        verifyNoInteractions(publisher);
    }

    @Test
    void publishFailurePropagatesSoTheOffsetIsNotAcknowledged() {
        statusUpdates(OrderStatus.FILLED);
        doThrow(new TradeEventPublishException("ORD-1", null)).when(publisher).publishOutcome(any());

        assertThrows(TradeEventPublishException.class, () -> saga.settle(order(OrderSide.BUY), FILL));
    }

    @Test
    void redeliveryAfterCrashRepeatsEveryStepWithTheSameOrderId() {
        // The first delivery dies after the debit and the redelivery reruns from the top.
        // The repeated debit carries the same orderId, which accounts-service treats as a no-op.
        doThrow(unavailable())
                .doNothing()
                .when(positionsClient).addPosition(anyString(), anyString(), anyString(), anyInt(), any());
        statusUpdates(OrderStatus.FILLED);

        assertThrows(HttpServerErrorException.class, () -> saga.settle(order(OrderSide.BUY), FILL));
        saga.settle(order(OrderSide.BUY), FILL);

        verify(accountsClient, times(2)).debit("ACC-1", "ORD-1", AMOUNT);
        verify(ordersClient).updateStatus("ORD-1", OrderStatus.FILLED, null);
        verify(publisher).publishOutcome(any());
    }
}
