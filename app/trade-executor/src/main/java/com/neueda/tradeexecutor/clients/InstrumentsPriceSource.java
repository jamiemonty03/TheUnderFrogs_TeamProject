package com.neueda.tradeexecutor.clients;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.neueda.tradeexecutor.dtos.InstrumentDto;
import com.neueda.tradeexecutor.dtos.PriceDto;
import com.neueda.tradeexecutor.services.PriceSource;

// Stand-in price source: the last price instruments-service stored, until a live price feed exists
@Component
public class InstrumentsPriceSource implements PriceSource {

    private final RestTemplate restTemplate;
    private final String pricesUrl;

    public InstrumentsPriceSource(RestTemplate restTemplate,
            @Value("${service.prices.url}") String pricesUrl) {
        this.restTemplate = restTemplate;
        this.pricesUrl = pricesUrl;
    }

    @Override
    public Optional<BigDecimal> getPrice(InstrumentDto instrument) {
        String path = pathFor(instrument.assetClass());
        if (path == null) {
            return Optional.empty();
        }

        try {
            PriceDto response = restTemplate.getForObject(
                pricesUrl + "/" + path + "/{symbol}", PriceDto.class, instrument.symbol());
            return Optional.ofNullable(response).map(PriceDto::price);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    private static String pathFor(String assetClass) {
        if (assetClass == null) {
            return null;
        }
        switch (assetClass) {
            case "Equity":
                return "stocks";
            case "ETF":
                return "etfs";
            case "Bond":
                return "bonds";
            default:
                return null;
        }
    }
}
