package com.neueda.tradeexecutor.clients;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

// Cash movements, all keyed by orderId (S7-5a): repeating a call with the same orderId is a no-op.
@Component
public class AccountsClient {

    // 400 insufficient funds, 403 account not active, 404 no such account
    private static final Set<Integer> BUSINESS_STATUSES = Set.of(400, 403, 404);

    private final RestTemplate restTemplate;
    private final String accountsServiceUrl;

    public AccountsClient(RestTemplate restTemplate,
            @Value("${service.accounts.url}") String accountsServiceUrl) {
        this.restTemplate = restTemplate;
        this.accountsServiceUrl = accountsServiceUrl;
    }

    public void debit(String accountId, String orderId, BigDecimal amount) {
        post("/{accountId}/debit", Map.of("orderId", orderId, "amount", amount), accountId);
    }

    public void credit(String accountId, String orderId, BigDecimal amount) {
        post("/{accountId}/credit", Map.of("orderId", orderId, "amount", amount), accountId);
    }

    public void reverse(String accountId, String orderId) {
        restTemplate.postForEntity(accountsServiceUrl + "/{accountId}/reversal",
                Map.of("orderId", orderId), Void.class, accountId);
    }

    private void post(String path, Map<String, Object> body, String accountId) {
        try {
            restTemplate.postForEntity(accountsServiceUrl + path, body, Void.class, accountId);
        } catch (HttpClientErrorException e) {
            throw BusinessRejections.translate(e, BUSINESS_STATUSES);
        }
    }
}
