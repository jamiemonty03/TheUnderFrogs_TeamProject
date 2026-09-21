package com.neueda.instrumentservice.repositories;

import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import com.neueda.instrumentservice.models.Instrument;

public class InMemoryInstrumentRepository implements InstrumentRepository {
    
    private final Map<String, Instrument> instruments = new HashMap<>();

    @Override
    public Instrument save(Instrument instrument) {
        
        instruments.put(instrument.getSymbol(), instrument);
        return instrument;
    }

    @Override
    public Optional<Instrument> findBySymbol(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(instruments.get(symbol));
    }

    @Override
    public boolean delete(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return false;
        }
        return instruments.remove(symbol) != null;
    }

    @Override
    public boolean exists(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            return false;
        }
        return instruments.containsKey(symbol);
    }

    public void clear() {
        instruments.clear();
    }

    public int count() {
        return instruments.size();
    }
}
