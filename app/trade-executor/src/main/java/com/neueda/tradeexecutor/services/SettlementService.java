package com.neueda.tradeexecutor.services;

import com.neueda.tradeexecutor.dtos.FillDecision;
import com.neueda.tradeexecutor.dtos.OrderDto;

// Acts on a fill or reject decision. The real implementation (moving cash and shares, setting the status) is S7-5.
public interface SettlementService {

    void settle(OrderDto order, FillDecision decision);
}
