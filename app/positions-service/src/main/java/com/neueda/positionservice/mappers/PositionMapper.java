package com.neueda.positionservice.mappers;

import com.neueda.positionservice.models.Position;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Optional;
import java.util.List;

@Mapper
public interface PositionMapper {

    int insertPosition(Position position);

    Position selectByAccountAndSymbol(@Param("accountId") String account, @Param("symbol") String symbol);

    List<Position> selectByAccount(@Param("accountId") String account);

    int updatePosition(Position position);

    int deleteByAccountAndSymbol(@Param("accountId") String account, @Param("symbol") String symbol);

    Integer countByAccountAndSymbol(@Param("accountId") String account, @Param("symbol") String symbol);
}