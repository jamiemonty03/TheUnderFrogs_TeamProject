package com.neueda.leap.repositories;

import java.util.Optional;
import com.neueda.leap.models.Instrument;

public interface InstrumentRepository {
    
    Instrument save(Instrument instrument);
    Optional<Instrument> findBySymbol(String symbol);
    boolean delete(String symbol);
    boolean exists(String symbol);
}
