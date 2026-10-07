package com.neueda.tradeexecutor.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import com.neueda.tradeexecutor.clients.AccountsClient;
import com.neueda.tradeexecutor.clients.OrdersClient;
import com.neueda.tradeexecutor.clients.PositionsClient;
import com.neueda.tradeexecutor.dtos.FillDecision;
import com.neueda.tradeexecutor.dtos.OrderDto;
import com.neueda.tradeexecutor.dtos.OrderOutcomePayload;
import com.neueda.tradeexecutor.enums.OrderSide;
import com.neueda.tradeexecutor.enums.OrderStatus;
import com.neueda.tradeexecutor.events.TradeEventPublisher;
import com.neueda.tradeexecutor.services.SagaSettlementService;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettlementSagaIntegrationTest {

    private static final String ACCOUNTS_URL = "http://accounts-service/api/accounts";
    private static final String POSITIONS_URL = "http://positions-service/api/positions";
    private static final String ORDERS_URL = "http://orders-service/api/orders";

    private MockRestServiceServer services;
    private TradeEventPublisher publisher;
    private SagaSettlementService saga;

    private static final OrderDto BUY = new OrderDto("ORD-1", "ACC-1", "AAPL", OrderSide.BUY, 10,
            new BigDecimal("150.00"), OrderStatus.NEW);

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        services = MockRestServiceServer.bindTo(restTemplate).build();
        publisher = mock(TradeEventPublisher.class);
        saga = new SagaSettlementService(
                new AccountsClient(restTemplate, ACCOUNTS_URL),
                new PositionsClient(restTemplate, POSITIONS_URL),
                new OrdersClient(restTemplate, ORDERS_URL),
                publisher);
    }

    @Test
    void positionStepFailureReversesTheCashAndRejectsTheOrder() {
        services.expect(ExpectedCount.once(), requestTo(ACCOUNTS_URL + "/ACC-1/debit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"orderId\":\"ORD-1\",\"amount\":1500.00}"))
                .andRespond(withSuccess());
        services.expect(ExpectedCount.once(), requestTo(POSITIONS_URL + "/ACC-1/AAPL/buy"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"POS-400\",\"message\":\"Position refused\"}"));
        services.expect(ExpectedCount.once(), requestTo(ACCOUNTS_URL + "/ACC-1/reversal"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"orderId\":\"ORD-1\"}"))
                .andRespond(withSuccess());
        services.expect(ExpectedCount.once(), requestTo(ORDERS_URL + "/ORD-1/status"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(jsonPath("$.expectedStatus").value("NEW"))
                .andExpect(jsonPath("$.newStatus").value("REJECTED"))
                .andExpect(jsonPath("$.reason").value("Position refused"))
                .andRespond(withSuccess());

        saga.settle(BUY, FillDecision.fill(new BigDecimal("150.00")));

        services.verify();
        ArgumentCaptor<OrderOutcomePayload> outcome = ArgumentCaptor.forClass(OrderOutcomePayload.class);
        verify(publisher).publishOutcome(outcome.capture());
        assertEquals(OrderStatus.REJECTED, outcome.getValue().status());
        assertEquals("Position refused", outcome.getValue().reason());
    }

    @Test
    void cancelledMidSettlementReversesBothMovementsAndPublishesNothing() {
        services.expect(requestTo(ACCOUNTS_URL + "/ACC-1/debit")).andRespond(withSuccess());
        services.expect(requestTo(POSITIONS_URL + "/ACC-1/AAPL/buy")).andRespond(withSuccess());
        services.expect(requestTo(ORDERS_URL + "/ORD-1/status"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"errorCode\":\"ORD-409\",\"message\":\"conflict\",\"currentStatus\":\"CANCELLED\"}"));
        services.expect(requestTo(POSITIONS_URL + "/ACC-1/AAPL/reversal"))
                .andExpect(content().json("{\"orderId\":\"ORD-1\"}"))
                .andRespond(withSuccess());
        services.expect(requestTo(ACCOUNTS_URL + "/ACC-1/reversal"))
                .andExpect(content().json("{\"orderId\":\"ORD-1\"}"))
                .andRespond(withSuccess());

        saga.settle(BUY, FillDecision.fill(new BigDecimal("150.00")));

        services.verify();
        verifyNoInteractions(publisher);
    }

    @Test
    void successfulBuyFillsAndPublishes() {
        services.expect(requestTo(ACCOUNTS_URL + "/ACC-1/debit")).andRespond(withSuccess());
        services.expect(requestTo(POSITIONS_URL + "/ACC-1/AAPL/buy"))
                .andExpect(content().json("{\"orderId\":\"ORD-1\",\"quantity\":10,\"price\":150.00}"))
                .andRespond(withSuccess());
        services.expect(requestTo(ORDERS_URL + "/ORD-1/status"))
                .andExpect(jsonPath("$.newStatus").value("FILLED"))
                .andRespond(withSuccess());

        saga.settle(BUY, FillDecision.fill(new BigDecimal("150.00")));

        services.verify();
        verify(publisher).publishOutcome(any());
    }
}
