package com.neueda.tradeexecutor.services;

import java.math.BigDecimal;
import java.util.Optional;
import com.neueda.tradeexecutor.dtos.InstrumentDto;
import com.neueda.tradeexecutor.enums.OrderSide;

public interface PriceSource {
    Optional<BigDecimal> getPrice(InstrumentDto instrument, OrderSide side);
}
