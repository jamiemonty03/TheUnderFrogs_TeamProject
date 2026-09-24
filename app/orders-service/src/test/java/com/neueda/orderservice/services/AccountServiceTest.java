package com.neueda.orderservice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.neueda.orderservice.enums.AccountStatus;
import com.neueda.orderservice.models.Account;

class AccountServiceTest {

    private static final String URL = "http://accounts-service:8081/api/accounts";

    private RestTemplate restTemplate;
    private AccountService accountService;
    private Account account;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        accountService = new AccountService(restTemplate, URL);
        account = new Account("ACC0001", "Alice", new BigDecimal("1000.00"), AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("debit POSTs the amount to accounts-service and syncs the returned balance")
    void debitCallsAccountsService() {
        Account updated = new Account("ACC0001", "Alice", new BigDecimal("700.00"), AccountStatus.ACTIVE);
        when(restTemplate.postForObject(URL + "/{accountId}/debit", Map.of("amount", new BigDecimal("300.00")),
                Account.class, "ACC0001")).thenReturn(updated);

        accountService.debit(account, new BigDecimal("300.00"));

        assertEquals(new BigDecimal("700.00"), account.getCashBalance());
    }

    @Test
    @DisplayName("credit POSTs the amount to accounts-service and syncs the returned balance")
    void creditCallsAccountsService() {
        Account updated = new Account("ACC0001", "Alice", new BigDecimal("1300.00"), AccountStatus.ACTIVE);
        when(restTemplate.postForObject(URL + "/{accountId}/credit", Map.of("amount", new BigDecimal("300.00")),
                Account.class, "ACC0001")).thenReturn(updated);

        accountService.credit(account, new BigDecimal("300.00"));

        assertEquals(new BigDecimal("1300.00"), account.getCashBalance());
    }

    @Test
    @DisplayName("debit propagates accounts-service errors and leaves the balance unchanged")
    void debitPropagatesErrors() {
        when(restTemplate.postForObject(eq(URL + "/{accountId}/debit"), eq(Map.of("amount", new BigDecimal("5000.00"))),
                eq(Account.class), eq("ACC0001"))).thenThrow(new RestClientException("400 Insufficient funds"));

        assertThrows(RestClientException.class, () -> accountService.debit(account, new BigDecimal("5000.00")));
        assertEquals(new BigDecimal("1000.00"), account.getCashBalance());
    }
}
