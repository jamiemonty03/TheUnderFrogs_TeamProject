package com.neueda.tradeexecutor.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import com.neueda.tradeexecutor.exceptions.UnknownOrderException;
import com.neueda.tradeexecutor.dtos.OrderDto;
import com.neueda.tradeexecutor.dtos.StatusUpdateRequest;
import com.neueda.tradeexecutor.dtos.StatusUpdateResult;
import com.neueda.tradeexecutor.enums.OrderStatus;

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
        try {
            return restTemplate.getForObject(ordersServiceUrl + "/{orderId}", OrderDto.class, orderId);
        } catch (HttpClientErrorException.NotFound e) {
            throw new UnknownOrderException(orderId, e);
        }
    }

    private record StatusConflictBody(OrderStatus currentStatus) {}

    public StatusUpdateResult updateStatus(String orderId, OrderStatus newStatus, String reason) {
        try {
            restTemplate.exchange(ordersServiceUrl + "/{orderId}/status", HttpMethod.PATCH,
                    new HttpEntity<>(new StatusUpdateRequest(OrderStatus.NEW, newStatus, reason)),
                    Void.class, orderId);
            return new StatusUpdateResult(true, newStatus);
        } catch (HttpClientErrorException.Conflict e) {
            return new StatusUpdateResult(false, e.getResponseBodyAs(StatusConflictBody.class).currentStatus());
        } catch (HttpClientErrorException.NotFound e) {
            throw new UnknownOrderException(orderId, e);
        }
    }
}
