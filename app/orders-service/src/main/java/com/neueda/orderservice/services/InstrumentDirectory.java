package com.neueda.orderservice.services;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.neueda.orderservice.exceptions.InstrumentServiceUnavailableException;
import com.neueda.orderservice.models.Instrument;

/**
 * Looks up instrument reference data owned by instruments-service.
 *
 * client_trades only stores the symbol, so filtering trades by instrument type
 * (asset class) means asking instruments-service which symbols are in that class.
 */
@Service
public class InstrumentDirectory {

    private static final Logger log = LoggerFactory.getLogger(InstrumentDirectory.class);


    public static final List<String> ASSET_CLASSES = List.of("Equity", "Bond", "ETF");

    private final RestTemplate restTemplate;

    @Value("${service.instruments.url:http://instruments-service:8081/api/instruments}")
    private String instrumentsServiceUrl;

    public InstrumentDirectory(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    public Set<String> symbolsInAssetClass(String assetClass) throws InstrumentServiceUnavailableException {
        String canonical = ASSET_CLASSES.stream()
            .filter(known -> known.equalsIgnoreCase(assetClass))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "assetClass must be one of " + String.join(", ", ASSET_CLASSES)));

        Instrument[] instruments;
        try {
            instruments = restTemplate.getForObject(instrumentsServiceUrl, Instrument[].class);
        } catch (RestClientException e) {
            log.warn("Instruments service lookup failed {} {}", kv("assetClass", canonical),
                kv("error", e.getClass().getSimpleName()));
            throw new InstrumentServiceUnavailableException("Instrument data is unavailable", e);
        }
        if (instruments == null) {
            return Set.of();
        }
        return Arrays.stream(instruments)
            .filter(instrument -> canonical.equalsIgnoreCase(instrument.getAssetClass()))
            .map(Instrument::getSymbol)
            .collect(Collectors.toSet());
    }
}
