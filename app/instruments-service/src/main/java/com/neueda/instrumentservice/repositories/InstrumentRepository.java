package com.neueda.instrumentservice.repositories;

import java.util.Optional;
import com.neueda.instrumentservice.models.Instrument;

public interface InstrumentRepository {
    
    Instrument save(Instrument instrument);
    Optional<Instrument> findBySymbol(String symbol);
    boolean delete(String symbol);
    boolean exists(String symbol);
}
