package com.neueda.accountservice.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.neueda.accountservice.exceptions.PaymentsUnavailableException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class StripePaymentGatewayTest {

    private final CheckoutRequest request = new CheckoutRequest(
        "ACC0011",
        25000L,
        "http://localhost:4200/wallet/return?session_id={CHECKOUT_SESSION_ID}",
        "http://localhost:4200/wallet/return?cancelled=1"
    );

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Without a secret key, payments are unavailable and Stripe is never called")
    void unavailableWithoutKey(String secretKey) {
        StripePaymentGateway gateway = new StripePaymentGateway(secretKey);

        assertThat(gateway.isAvailable()).isFalse();
        assertThatThrownBy(() -> gateway.createCheckout(request))
            .isInstanceOf(PaymentsUnavailableException.class);
        assertThatThrownBy(() -> gateway.retrieveCheckout("cs_test_123"))
            .isInstanceOf(PaymentsUnavailableException.class);
    }

    @Test
    @DisplayName("With a secret key, payments are available")
    void availableWithKey() {
        assertThat(new StripePaymentGateway("sk_test_dummy").isAvailable()).isTrue();
    }

    @Test
    @DisplayName("Builds a one-off USD payment for the amount, tagged with the account")
    void checkoutParams() {
        SessionCreateParams params = StripePaymentGateway.checkoutParams(request);
        SessionCreateParams.LineItem item = params.getLineItems().get(0);

        assertThat(params.getMode()).isEqualTo(SessionCreateParams.Mode.PAYMENT);
        assertThat(params.getSuccessUrl()).isEqualTo(request.successUrl());
        assertThat(params.getCancelUrl()).isEqualTo(request.cancelUrl());
        assertThat(params.getClientReferenceId()).isEqualTo("ACC0011");
        assertThat(params.getMetadata()).containsEntry("accountId", "ACC0011");
        assertThat(params.getLineItems()).hasSize(1);
        assertThat(item.getQuantity()).isEqualTo(1L);
        assertThat(item.getPriceData().getCurrency()).isEqualTo("usd");
        assertThat(item.getPriceData().getUnitAmount()).isEqualTo(25000L);
        assertThat(item.getPriceData().getProductData().getName()).isEqualTo("Toadstock cash top-up");
    }

    @Test
    @DisplayName("Reads the payment status, amount, payment intent and account from a Stripe session")
    void mapsSession() {
        Session session = new Session();
        session.setId("cs_test_123");
        session.setUrl("https://checkout.stripe.com/c/pay/cs_test_123");
        session.setPaymentStatus("paid");
        session.setPaymentIntent("pi_123");
        session.setAmountTotal(25000L);
        session.setCurrency("usd");
        session.setMetadata(Map.of("accountId", "ACC0011"));

        CheckoutSession mapped = StripePaymentGateway.toCheckoutSession(session);

        assertThat(mapped).isEqualTo(new CheckoutSession(
            "cs_test_123",
            "https://checkout.stripe.com/c/pay/cs_test_123",
            "paid",
            "pi_123",
            25000L,
            "usd",
            "ACC0011"
        ));
        assertThat(mapped.isPaid()).isTrue();
    }

    @Test
    @DisplayName("An unpaid session or one without metadata is handled safely")
    void mapsUnpaidSessionWithoutMetadata() {
        Session session = new Session();
        session.setId("cs_test_456");
        session.setPaymentStatus("unpaid");

        CheckoutSession mapped = StripePaymentGateway.toCheckoutSession(session);

        assertThat(mapped.isPaid()).isFalse();
        assertThat(mapped.accountId()).isNull();
        assertThat(mapped.paymentIntentId()).isNull();
    }
}
