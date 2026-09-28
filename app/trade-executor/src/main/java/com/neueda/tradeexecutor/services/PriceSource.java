package com.neueda.tradeexecutor.services;

import java.math.BigDecimal;
import java.util.Optional;
import com.neueda.tradeexecutor.dtos.InstrumentDto;

public interface PriceSource {

    Optional<BigDecimal> getPrice(InstrumentDto instrument);
}
