package com.neueda.tradeexecutor.services;

import com.neueda.tradeexecutor.dtos.FillDecision;
import com.neueda.tradeexecutor.dtos.OrderDto;

public interface SettlementService {

    void settle(OrderDto order, FillDecision decision);

    void compensateCancelled(OrderDto order);
}
