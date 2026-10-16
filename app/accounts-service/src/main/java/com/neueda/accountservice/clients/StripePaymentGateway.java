package com.neueda.accountservice.clients;

import com.neueda.accountservice.exceptions.PaymentGatewayException;
import com.neueda.accountservice.exceptions.PaymentNotFoundException;
import com.neueda.accountservice.exceptions.PaymentsUnavailableException;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StripePaymentGateway implements PaymentGateway {

    static final String CURRENCY = "usd";
    static final String ACCOUNT_ID_KEY = "accountId";
    static final String PRODUCT_NAME = "Toadstock cash top-up";

    private final StripeClient client;

    public StripePaymentGateway(@Value("${stripe.secret-key:}") String secretKey) {
        this.client = secretKey == null || secretKey.isBlank() ? null : new StripeClient(secretKey);
    }

    @Override
    public boolean isAvailable() {
        return client != null;
    }

    @Override
    public CheckoutSession createCheckout(CheckoutRequest request) {
        try {
            return toCheckoutSession(requireClient().v1().checkout().sessions().create(checkoutParams(request)));
        } catch (StripeException e) {
            throw new PaymentGatewayException("Could not start the payment", e);
        }
    }

    @Override
    public CheckoutSession retrieveCheckout(String sessionId) {
        try {
            return toCheckoutSession(requireClient().v1().checkout().sessions().retrieve(sessionId));
        } catch (StripeException e) {
            throw retrieveFailure(e);
        }
    }

    static RuntimeException retrieveFailure(StripeException e) {
        if (Integer.valueOf(404).equals(e.getStatusCode())) {
            return new PaymentNotFoundException();
        }
        return new PaymentGatewayException("Could not check the payment", e);
    }

    static SessionCreateParams checkoutParams(CheckoutRequest request) {
        return SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl(request.successUrl())
            .setCancelUrl(request.cancelUrl())
            .setClientReferenceId(request.accountId())
            .putMetadata(ACCOUNT_ID_KEY, request.accountId())
            .addLineItem(SessionCreateParams.LineItem.builder()
                .setQuantity(1L)
                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                    .setCurrency(CURRENCY)
                    .setUnitAmount(request.amountCents())
                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName(PRODUCT_NAME)
                        .build())
                    .build())
                .build())
            .build();
    }

    static CheckoutSession toCheckoutSession(Session session) {
        Map<String, String> metadata = session.getMetadata();
        return new CheckoutSession(
            session.getId(),
            session.getUrl(),
            session.getPaymentStatus(),
            session.getPaymentIntent(),
            session.getAmountTotal(),
            session.getCurrency(),
            metadata == null ? null : metadata.get(ACCOUNT_ID_KEY)
        );
    }

    private StripeClient requireClient() {
        if (client == null) {
            throw new PaymentsUnavailableException();
        }
        return client;
    }
}
