package com.neueda.orderservice.services;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.neueda.orderservice.models.Account;

/**
 * Client for accounts-service. Debits and credits are applied by accounts-service,
 * which also enforces active status and sufficient funds; any failure surfaces
 * as a RestClientException.
 */
@Service
public class AccountService {

    private final RestTemplate restTemplate;
    private final String accountsServiceUrl;

    public AccountService(RestTemplate restTemplate,
            @Value("${service.accounts.url:http://accounts-service:8081/api/accounts}") String accountsServiceUrl) {
        this.restTemplate = restTemplate;
        this.accountsServiceUrl = accountsServiceUrl;
    }

    public Account getAccountById(String accountId) {
        return restTemplate.getForObject(accountsServiceUrl + "/{accountId}", Account.class, accountId);
    }

    public void debit(Account account, BigDecimal amount) {
        Account updated = restTemplate.postForObject(
            accountsServiceUrl + "/{accountId}/debit",
            Map.of("amount", amount),
            Account.class,
            account.getAccountId()
        );
        syncBalance(account, updated);
    }

    public void credit(Account account, BigDecimal amount) {
        Account updated = restTemplate.postForObject(
            accountsServiceUrl + "/{accountId}/credit",
            Map.of("amount", amount),
            Account.class,
            account.getAccountId()
        );
        syncBalance(account, updated);
    }

    private void syncBalance(Account account, Account updated) {
        if (updated != null) {
            account.setCashBalance(updated.getCashBalance());
        }
    }
}
