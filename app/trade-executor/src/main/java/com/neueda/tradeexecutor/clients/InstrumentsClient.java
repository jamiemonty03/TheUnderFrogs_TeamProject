package com.neueda.tradeexecutor.clients;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.neueda.tradeexecutor.dtos.InstrumentDto;

@Component
public class InstrumentsClient {

    private final RestTemplate restTemplate;
    private final String instrumentsServiceUrl;

    public InstrumentsClient(RestTemplate restTemplate,
            @Value("${service.instruments.url}") String instrumentsServiceUrl) {
        this.restTemplate = restTemplate;
        this.instrumentsServiceUrl = instrumentsServiceUrl;
    }

    public Optional<InstrumentDto> getInstrument(String symbol) {
        try {
            return Optional.ofNullable(
                restTemplate.getForObject(instrumentsServiceUrl + "/{symbol}", InstrumentDto.class, symbol));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    public List<String> getTradableSymbols() {
        InstrumentDto[] instruments = restTemplate.getForObject(instrumentsServiceUrl, InstrumentDto[].class);
        if (instruments == null) {
            return List.of();
        }
        return Arrays.stream(instruments)
                .filter(InstrumentDto::tradable)
                .map(InstrumentDto::symbol)
                .toList();
    }
}
