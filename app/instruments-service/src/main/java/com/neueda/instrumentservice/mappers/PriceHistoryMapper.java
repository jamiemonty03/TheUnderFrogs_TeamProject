package com.neueda.instrumentservice.mappers;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.neueda.instrumentservice.dtos.responses.PricePointResponse;

@Mapper
public interface PriceHistoryMapper {
    List<PricePointResponse> findRecent(@Param("symbol") String symbol, @Param("days") int days);
}
