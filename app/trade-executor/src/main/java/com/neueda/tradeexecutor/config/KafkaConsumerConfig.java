package com.neueda.tradeexecutor.config;

import java.nio.charset.StandardCharsets;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.ExponentialBackOff;
import com.neueda.tradeexecutor.events.Topics;
import com.neueda.tradeexecutor.exceptions.UnknownOrderException;

@Configuration
public class KafkaConsumerConfig {

    public static final String DLT_ATTEMPT_COUNT = "x-attempt-count";

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, retryBackOff());
        handler.addNotRetryableExceptions(
                DeserializationException.class,
                IllegalArgumentException.class,
                com.fasterxml.jackson.core.JsonProcessingException.class,
                UnknownOrderException.class);
        return handler;
    }

    static ExponentialBackOff retryBackOff() {
        ExponentialBackOff backOff = new ExponentialBackOff(1_000L, 2.0);
        backOff.setMaxInterval(8_000L);
        backOff.setMaxElapsedTime(15_000L);
        return backOff;
    }

    static int attemptCount(org.apache.kafka.common.header.Headers headers) {
        var attemptHeader = headers.lastHeader(KafkaHeaders.DELIVERY_ATTEMPT);
        if (attemptHeader == null || attemptHeader.value() == null || attemptHeader.value().length != Integer.BYTES) return 1;
        return java.nio.ByteBuffer.wrap(attemptHeader.value()).getInt();
    }

    @Bean
    public ProducerFactory<String, Object> dltProducerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        var valueSerializer = new DelegatingByTypeSerializer(Map.of(
                byte[].class, new ByteArraySerializer(),
                Object.class, new JsonSerializer<>()), true);
        return new DefaultKafkaProducerFactory<>(
                Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers),
                new StringSerializer(), valueSerializer);
    }

    @Bean
    public KafkaTemplate<String, Object> dltKafkaTemplate(ProducerFactory<String, Object> dltProducerFactory) {
        return new KafkaTemplate<>(dltProducerFactory);
    }

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new org.apache.kafka.common.TopicPartition(Topics.ORDERS_DLT, record.partition()));
        recoverer.setHeadersFunction((record, exception) -> {
            int attempts = attemptCount(record.headers());
            return new org.apache.kafka.common.header.internals.RecordHeaders().add(
                    new RecordHeader(DLT_ATTEMPT_COUNT, Integer.toString(attempts).getBytes(StandardCharsets.UTF_8)));
        });
        return recoverer;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConsumerFactory<Object, Object> consumerFactory, DefaultErrorHandler kafkaErrorHandler) {
        var factory = new ConcurrentKafkaListenerContainerFactory<Object, Object>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.getContainerProperties().setAckMode(
                org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL);
        factory.getContainerProperties().setDeliveryAttemptHeader(true);
        return factory;
    }
}
