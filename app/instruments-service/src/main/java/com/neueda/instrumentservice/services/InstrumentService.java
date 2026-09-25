package com.neueda.instrumentservice.services;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.neueda.instrumentservice.dtos.responses.InstrumentResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.models.Instrument;
import com.neueda.instrumentservice.mappers.InstrumentMapper;
import com.neueda.instrumentservice.mappers.TrackedTickerMapper;

@Service
public class InstrumentService {

    private static final Logger log = LoggerFactory.getLogger(InstrumentService.class);

    private final InstrumentMapper instrumentMapper;
    private final TrackedTickerMapper trackedTickerMapper;

    public InstrumentService(InstrumentMapper instrumentMapper,
                             TrackedTickerMapper trackedTickerMapper) {
        this.instrumentMapper = instrumentMapper;
        this.trackedTickerMapper = trackedTickerMapper;
    }

    public List<InstrumentResponse> getAllInstruments() {
        return instrumentMapper.findAll().stream()
                .map(InstrumentService::toResponse)
                .toList();
    }

    public InstrumentResponse getInstrumentBySymbol(String symbol) throws InstrumentNotFoundException {
        Instrument instrument = instrumentMapper.findBySymbol(symbol)
                .orElseThrow(() -> new InstrumentNotFoundException(symbol));
        return toResponse(instrument);
    }

    /**
     * Deletes the instrument and deactivates its tracked_tickers row, so the
     * next ETL run doesn't fetch and re-insert it.
     */
    @Transactional
    public void deleteInstrument(String symbol) throws InstrumentNotFoundException {
        if (!instrumentMapper.exists(symbol)) {
            throw new InstrumentNotFoundException(symbol);
        }
        instrumentMapper.delete(symbol);
        if (trackedTickerMapper.deactivate(symbol) == 0) {
            log.warn("Deleted instrument {} had no tracked_tickers row to deactivate", symbol);
        }
    }

    private static InstrumentResponse toResponse(Instrument instrument) {
        return new InstrumentResponse(
                instrument.getSymbol(),
                instrument.getName(),
                instrument.getAssetClass(),
                instrument.getCurrency(),
                instrument.getExchange(),
                instrument.isTradable()
        );
    }
}
