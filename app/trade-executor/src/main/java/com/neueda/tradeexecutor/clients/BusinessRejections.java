package com.neueda.tradeexecutor.clients;

import java.util.Set;
import org.springframework.web.client.HttpClientErrorException;
import com.neueda.tradeexecutor.exceptions.SettlementRejectedException;


final class BusinessRejections {

    private record ErrorBody(String code, String message) {}

    private BusinessRejections() {
    }

    static RuntimeException translate(HttpClientErrorException e, Set<Integer> businessStatuses) {
        if (!businessStatuses.contains(e.getStatusCode().value())) {
            return e;
        }
        return new SettlementRejectedException(reason(e), e);
    }

    private static String reason(HttpClientErrorException e) {
        try {
            ErrorBody body = e.getResponseBodyAs(ErrorBody.class);
            if (body != null && body.message() != null && !body.message().isBlank()) {
                return body.message();
            }
        } catch (RuntimeException ignored) {
        }
        return "Refused with HTTP " + e.getStatusCode().value();
    }
}
