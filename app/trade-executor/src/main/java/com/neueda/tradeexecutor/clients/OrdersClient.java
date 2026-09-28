package com.neueda.tradeexecutor.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.neueda.tradeexecutor.dtos.OrderDto;

@Component
public class OrdersClient {

    private final RestTemplate restTemplate;
    private final String ordersServiceUrl;

    public OrdersClient(RestTemplate restTemplate,
            @Value("${service.orders.url}") String ordersServiceUrl) {
        this.restTemplate = restTemplate;
        this.ordersServiceUrl = ordersServiceUrl;
    }

    public OrderDto getOrder(String orderId) {
        return restTemplate.getForObject(ordersServiceUrl + "/{orderId}", OrderDto.class, orderId);
    }
}
