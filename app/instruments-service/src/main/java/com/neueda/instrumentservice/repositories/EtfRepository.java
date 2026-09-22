package com.neueda.instrumentservice.repositories;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import com.neueda.instrumentservice.models.Etf;

@Mapper
public interface EtfRepository {
    List<Etf> findAll();
    Optional<Etf> findBySymbol(String symbol);
}
