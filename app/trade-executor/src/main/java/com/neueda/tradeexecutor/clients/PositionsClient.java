package com.neueda.tradeexecutor.clients;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


@Component
public class PositionsClient {


    private static final Set<Integer> BUSINESS_STATUSES = Set.of(400, 404, 409);

    private final RestTemplate restTemplate;
    private final String positionsServiceUrl;

    public PositionsClient(RestTemplate restTemplate,
            @Value("${service.positions.url}") String positionsServiceUrl) {
        this.restTemplate = restTemplate;
        this.positionsServiceUrl = positionsServiceUrl;
    }

    public void addPosition(String accountId, String symbol, String orderId, int quantity, BigDecimal price) {
        post("/{accountId}/{symbol}/buy",
                Map.of("orderId", orderId, "quantity", quantity, "price", price), accountId, symbol);
    }

    public void reducePosition(String accountId, String symbol, String orderId, int quantity) {
        post("/{accountId}/{symbol}/sell",
                Map.of("orderId", orderId, "quantity", quantity), accountId, symbol);
    }

    
    public void reverse(String accountId, String symbol, String orderId) {
        restTemplate.postForEntity(positionsServiceUrl + "/{accountId}/{symbol}/reversal",
                Map.of("orderId", orderId), Void.class, accountId, symbol);
    }

    private void post(String path, Map<String, Object> body, String accountId, String symbol) {
        try {
            restTemplate.postForEntity(positionsServiceUrl + path, body, Void.class, accountId, symbol);
        } catch (HttpClientErrorException e) {
            throw BusinessRejections.translate(e, BUSINESS_STATUSES);
        }
    }
}
