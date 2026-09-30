package com.neueda.tradeexecutor.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.util.ReflectionTestUtils.invokeMethod;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

class KafkaConsumerConfigTest {
    private final KafkaConsumerConfig config = new KafkaConsumerConfig();

    @Test
    void listenerFactoryUsesManualAcknowledgmentAndTracksAttempts() {
        var handler = config.kafkaErrorHandler(mock(DeadLetterPublishingRecoverer.class));
        var factory = config.kafkaListenerContainerFactory(
                mock(org.springframework.kafka.core.ConsumerFactory.class), handler);
        assertEquals(org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL,
                factory.getContainerProperties().getAckMode());
        assertTrue(handler.deliveryAttemptHeader());
    }

    @Test
    void retriesGrowAndStopAfterTheConfiguredBudget() {
        var execution = KafkaConsumerConfig.retryBackOff().start();
        assertEquals(1_000L, execution.nextBackOff());
        assertEquals(2_000L, execution.nextBackOff());
        assertEquals(4_000L, execution.nextBackOff());
        assertEquals(8_000L, execution.nextBackOff());
        assertEquals(org.springframework.util.backoff.BackOffExecution.STOP, execution.nextBackOff());
    }

    @Test
    void attemptHeaderDefaultsToOneAndTracksTheCurrentDeliveryAttempt() {
        var headers = new org.apache.kafka.common.header.internals.RecordHeaders();
        assertEquals(1, KafkaConsumerConfig.attemptCount(headers));
        headers.add(org.springframework.kafka.support.KafkaHeaders.DELIVERY_ATTEMPT,
                java.nio.ByteBuffer.allocate(Integer.BYTES).putInt(5).array());
        assertEquals(5, KafkaConsumerConfig.attemptCount(headers));
    }

    @Test
    void unknownOrdersAreNotRetryable() {
        var handler = configuredHandler();
        assertFalse(isRetryable(handler, new UnknownOrderException("missing", null)));
    }

    @Test
    void transportFailuresAndRateLimitsAreRetryable() {
        var handler = configuredHandler();
        assertTrue(isRetryable(handler, new ResourceAccessException("timeout")));
        assertTrue(isRetryable(handler, HttpClientErrorException.TooManyRequests.create(
                org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "rate limited", org.springframework.http.HttpHeaders.EMPTY,
                new byte[0], java.nio.charset.StandardCharsets.UTF_8)));
    }

    private org.springframework.kafka.listener.DefaultErrorHandler configuredHandler() {
        return config.kafkaErrorHandler(mock(DeadLetterPublishingRecoverer.class));
    }

    private static boolean isRetryable(org.springframework.kafka.listener.DefaultErrorHandler handler, Throwable failure) {
        var classifier = invokeMethod(handler, "getClassifier");
        return (Boolean) invokeMethod(classifier, "classify", failure);
    }
}
