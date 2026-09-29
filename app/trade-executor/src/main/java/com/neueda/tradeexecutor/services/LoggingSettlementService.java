package com.neueda.tradeexecutor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.neueda.tradeexecutor.dtos.FillDecision;
import com.neueda.tradeexecutor.dtos.OrderDto;

// Temporary stand-in until S7-5: logs the decision instead of settling it
@Service
public class LoggingSettlementService implements SettlementService {

    private static final Logger log = LoggerFactory.getLogger(LoggingSettlementService.class);

    @Override
    public void settle(OrderDto order, FillDecision decision) {
        if (decision.filled()) {
            log.info("Order {} FILL: {} {} {} @ {}", order.orderId(), order.side(), order.quantity(),
                    order.symbol(), decision.fillPrice());
        } else {
            log.info("Order {} REJECT: {}", order.orderId(), decision.reason());
        }
    }
}
