package com.neueda.instrumentservice.repositories;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import com.neueda.instrumentservice.models.Instrument;

@Mapper
public interface InstrumentRepository {
    void save(Instrument instrument);
    Optional<Instrument> findBySymbol(String symbol);
    List<Instrument> findAll();
    void delete(String symbol);
    boolean exists(String symbol);
    void update(Instrument instrument);
}
