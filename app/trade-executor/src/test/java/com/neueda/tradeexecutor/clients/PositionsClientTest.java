package com.neueda.tradeexecutor.clients;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import com.neueda.tradeexecutor.exceptions.SettlementRejectedException;

class PositionsClientTest {

    private static final String POSITIONS_URL = "http://positions-service/api/positions";

    private MockRestServiceServer positionsService;
    private PositionsClient positionsClient;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        positionsService = MockRestServiceServer.bindTo(restTemplate).build();
        positionsClient = new PositionsClient(restTemplate, POSITIONS_URL);
    }

    @Test
    void addPositionSendsQuantityPriceAndOrderId() {
        positionsService.expect(requestTo(POSITIONS_URL + "/ACC-1/AAPL/buy"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"orderId\":\"ORD-1\",\"quantity\":10,\"price\":150.00}"))
                .andRespond(withSuccess());

        positionsClient.addPosition("ACC-1", "AAPL", "ORD-1", 10, new BigDecimal("150.00"));

        positionsService.verify();
    }

    @Test
    void reducePositionSendsQuantityAndOrderId() {
        positionsService.expect(requestTo(POSITIONS_URL + "/ACC-1/AAPL/sell"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"orderId\":\"ORD-1\",\"quantity\":10}"))
                .andRespond(withSuccess());

        positionsClient.reducePosition("ACC-1", "AAPL", "ORD-1", 10);

        positionsService.verify();
    }

    @Test
    void reverseSendsOrderId() {
        positionsService.expect(requestTo(POSITIONS_URL + "/ACC-1/AAPL/reversal"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"orderId\":\"ORD-1\"}"))
                .andRespond(withSuccess());

        positionsClient.reverse("ACC-1", "AAPL", "ORD-1");

        positionsService.verify();
    }

    @Test
    void insufficientHoldingsIsABusinessRejection() {
        positionsService.expect(requestTo(POSITIONS_URL + "/ACC-1/AAPL/sell"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"POS-409\",\"message\":\"Insufficient holdings\"}"));

        SettlementRejectedException e = assertThrows(SettlementRejectedException.class,
                () -> positionsClient.reducePosition("ACC-1", "AAPL", "ORD-1", 10));

        assertEquals("Insufficient holdings", e.getMessage());
    }

    @Test
    void noPositionToSellIsABusinessRejection() {
        positionsService.expect(requestTo(POSITIONS_URL + "/ACC-1/AAPL/sell"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(SettlementRejectedException.class,
                () -> positionsClient.reducePosition("ACC-1", "AAPL", "ORD-1", 10));
    }

    @Test
    void serverErrorPropagatesSoKafkaRetries() {
        positionsService.expect(requestTo(POSITIONS_URL + "/ACC-1/AAPL/buy"))
                .andRespond(withServerError());

        assertThrows(HttpServerErrorException.class,
                () -> positionsClient.addPosition("ACC-1", "AAPL", "ORD-1", 10, BigDecimal.ONE));
    }
}
