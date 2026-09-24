package com.neueda.orderservice.clients;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.neueda.orderservice.models.Account;

/**
 * HTTP client for the accounts-service REST API.
 *
 * <p>This is <b>not</b> an account service. orders-service does not own account data,
 * has no access to accounts-db and holds no account business rules. Each method is a
 * thin wrapper around one accounts-service endpoint:
 * <ul>
 *   <li>{@link #getAccountById} - {@code GET  /accounts/{accountId}}</li>
 *   <li>{@link #debit}          - {@code POST /accounts/{accountId}/debit}</li>
 *   <li>{@link #credit}         - {@code POST /accounts/{accountId}/credit}</li>
 * </ul>
 *
 * <p>accounts-service remains the single source of truth: it applies the balance change
 * and enforces active status, sufficient funds and optimistic locking. Any rejection or
 * connectivity failure surfaces here as a {@link org.springframework.web.client.RestClientException}.
 *
 * <p>The base URL comes from {@code service.accounts.url}.
 */
@Component
public class AccountsClient {

    private final RestTemplate restTemplate;
    private final String accountsServiceUrl;

    public AccountsClient(RestTemplate restTemplate,
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

    /** Copies the balance accounts-service returned onto the caller's local snapshot. */
    private void syncBalance(Account account, Account updated) {
        if (updated != null) {
            account.setCashBalance(updated.getCashBalance());
        }
    }
}
