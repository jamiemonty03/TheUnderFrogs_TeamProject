package com.neueda.instrumentservice.mappers;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import com.neueda.instrumentservice.models.Etf;

@Mapper
public interface EtfMapper {
    List<Etf> findAll();
    Optional<Etf> findBySymbol(String symbol);
}
