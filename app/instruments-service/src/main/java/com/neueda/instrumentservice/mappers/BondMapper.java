package com.neueda.instrumentservice.mappers;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import com.neueda.instrumentservice.models.Bond;

@Mapper
public interface BondMapper {
    List<Bond> findAll();
    Optional<Bond> findBySymbol(String symbol);
}
