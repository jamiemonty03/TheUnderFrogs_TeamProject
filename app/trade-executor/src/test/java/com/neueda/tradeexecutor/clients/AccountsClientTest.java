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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import com.neueda.tradeexecutor.exceptions.SettlementRejectedException;

class AccountsClientTest {

    private static final String ACCOUNTS_URL = "http://accounts-service/api/accounts";

    private MockRestServiceServer accountsService;
    private AccountsClient accountsClient;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        accountsService = MockRestServiceServer.bindTo(restTemplate).build();
        accountsClient = new AccountsClient(restTemplate, ACCOUNTS_URL);
    }

    @Test
    void debitSendsAmountAndOrderId() {
        accountsService.expect(requestTo(ACCOUNTS_URL + "/ACC-1/debit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"orderId\":\"ORD-1\",\"amount\":1500.00}"))
                .andRespond(withSuccess());

        accountsClient.debit("ACC-1", "ORD-1", new BigDecimal("1500.00"));

        accountsService.verify();
    }

    @Test
    void creditSendsAmountAndOrderId() {
        accountsService.expect(requestTo(ACCOUNTS_URL + "/ACC-1/credit"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"orderId\":\"ORD-1\",\"amount\":1500.00}"))
                .andRespond(withSuccess());

        accountsClient.credit("ACC-1", "ORD-1", new BigDecimal("1500.00"));

        accountsService.verify();
    }

    @Test
    void reverseSendsOrderId() {
        accountsService.expect(requestTo(ACCOUNTS_URL + "/ACC-1/reversal"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"orderId\":\"ORD-1\"}"))
                .andRespond(withSuccess());

        accountsClient.reverse("ACC-1", "ORD-1");

        accountsService.verify();
    }

    @Test
    void insufficientFundsIsABusinessRejectionCarryingTheServiceMessage() {
        accountsService.expect(requestTo(ACCOUNTS_URL + "/ACC-1/debit"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"ORD-400\",\"message\":\"Insufficient funds. Balance: 10.00\"}"));

        SettlementRejectedException e = assertThrows(SettlementRejectedException.class,
                () -> accountsClient.debit("ACC-1", "ORD-1", new BigDecimal("1500.00")));

        assertEquals("Insufficient funds. Balance: 10.00", e.getMessage());
    }

    @Test
    void inactiveAccountIsABusinessRejection() {
        accountsService.expect(requestTo(ACCOUNTS_URL + "/ACC-1/credit"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        SettlementRejectedException e = assertThrows(SettlementRejectedException.class,
                () -> accountsClient.credit("ACC-1", "ORD-1", BigDecimal.TEN));

        assertEquals("Refused with HTTP 403", e.getMessage());
    }

    @Test
    void unauthorisedIsNotABusinessRejection() {
        accountsService.expect(requestTo(ACCOUNTS_URL + "/ACC-1/debit"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(HttpClientErrorException.Unauthorized.class,
                () -> accountsClient.debit("ACC-1", "ORD-1", BigDecimal.TEN));
    }

    @Test
    void serverErrorPropagatesSoKafkaRetries() {
        accountsService.expect(requestTo(ACCOUNTS_URL + "/ACC-1/debit"))
                .andRespond(withServerError());

        assertThrows(HttpServerErrorException.class,
                () -> accountsClient.debit("ACC-1", "ORD-1", BigDecimal.TEN));
    }

    @Test
    void reversalErrorsAreNeverBusinessRejections() {
        accountsService.expect(requestTo(ACCOUNTS_URL + "/ACC-1/reversal"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThrows(HttpClientErrorException.BadRequest.class,
                () -> accountsClient.reverse("ACC-1", "ORD-1"));
    }
}
