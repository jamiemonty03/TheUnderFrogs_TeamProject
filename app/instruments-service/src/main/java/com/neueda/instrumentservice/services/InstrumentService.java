package com.neueda.instrumentservice.services;

import java.util.List;
import org.springframework.stereotype.Service;
import com.neueda.instrumentservice.dtos.responses.InstrumentResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.models.Instrument;
import com.neueda.instrumentservice.repositories.InstrumentRepository;

@Service
public class InstrumentService {

    private final InstrumentRepository instrumentRepository;

    public InstrumentService(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    public List<InstrumentResponse> getAllInstruments() {
        return instrumentRepository.findAll().stream()
                .map(InstrumentService::toResponse)
                .toList();
    }

    public InstrumentResponse getInstrumentBySymbol(String symbol) throws InstrumentNotFoundException {
        Instrument instrument = instrumentRepository.findBySymbol(symbol)
                .orElseThrow(() -> new InstrumentNotFoundException(symbol));
        return toResponse(instrument);
    }

    public void deleteInstrument(String symbol) throws InstrumentNotFoundException {
        if (!instrumentRepository.exists(symbol)) {
            throw new InstrumentNotFoundException(symbol);
        }
        instrumentRepository.delete(symbol);
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
