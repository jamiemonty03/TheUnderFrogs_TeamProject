package com.neueda.orderservice.services;

import com.neueda.orderservice.models.ClientTrade;
import com.neueda.orderservice.models.ClientTradeFilter;
import com.neueda.orderservice.models.PositionStep;
import com.neueda.orderservice.repositories.ClientTradeRepository;
import com.neueda.orderservice.enums.OrderSide;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing client trade historical data.
 * 
 * Provides business logic for querying and calculating metrics from
 * historical trade records stored in the client_trades table.
 * Used for reconciliation and weighted average cost calculations.
 */
@Service
public class ClientTradeService {

    // Same order as the derived account history queries
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("tradeDate"), Sort.Order.desc("tradeId"));

    private final ClientTradeRepository clientTradeRepository;

    public ClientTradeService(ClientTradeRepository clientTradeRepository) {
        if (clientTradeRepository == null) {
            throw new IllegalArgumentException("ClientTradeRepository cannot be null");
        }
        this.clientTradeRepository = clientTradeRepository;
    }

    public List<ClientTrade> getAccountHistory(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("AccountId cannot be null or blank");
        }
        return clientTradeRepository.findByAccountIdOrderByTradeDateDescTradeIdDesc(accountId);
    }



    public List<ClientTrade> search(ClientTradeFilter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("Filter cannot be null");
        }
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new IllegalArgumentException("'from' date must be on or before 'to' date");
        }
        if (filter.symbols() != null && filter.symbols().isEmpty()) {
            return List.of();
        }

        Specification<ClientTrade> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.accountId() != null) {
                predicates.add(cb.equal(root.get("accountId"), filter.accountId()));
            }
            if (filter.from() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("tradeDate"), filter.from()));
            }
            if (filter.to() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("tradeDate"), filter.to()));
            }
            if (filter.symbols() != null) {
                predicates.add(root.get("symbol").in(filter.symbols()));
            }
            if (filter.side() != null) {
                predicates.add(cb.equal(root.get("tradeType"), filter.side()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return clientTradeRepository.findAll(spec, NEWEST_FIRST);
    }


    public List<ClientTrade> getSymbolHistory(String accountId, String symbol) {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("AccountId cannot be null or blank");
        }
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
        return clientTradeRepository.findByAccountIdAndSymbolOrderByTradeDateDescTradeIdDesc(accountId, symbol);
    }


    public BigDecimal getHistoricalBuyQuantity(String accountId, String symbol) {
        List<ClientTrade> trades = getSymbolHistory(accountId, symbol);
        return trades.stream()
            .filter(trade -> trade.getTradeType() == OrderSide.BUY)
            .map(ClientTrade::getQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    public BigDecimal getHistoricalSellQuantity(String accountId, String symbol) {
        List<ClientTrade> trades = getSymbolHistory(accountId, symbol);
        return trades.stream()
            .filter(trade -> trade.getTradeType() == OrderSide.SELL)
            .map(ClientTrade::getQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getWeightedAverageCost(String accountId, String symbol) {
        List<ClientTrade> trades = getSymbolHistory(accountId, symbol);
        
        List<ClientTrade> buyTrades = trades.stream()
            .filter(trade -> trade.getTradeType() == OrderSide.BUY)
            .toList();

        if (buyTrades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalCost = buyTrades.stream()
            .map(trade -> trade.getQuantity().multiply(trade.getPrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalQuantity = buyTrades.stream()
            .map(ClientTrade::getQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return totalCost.divide(totalQuantity, 2, java.math.RoundingMode.HALF_UP);
    }

    public boolean reconcileWithPosition(String accountId, String symbol, BigDecimal currentPositionQuantity) {
        if (currentPositionQuantity == null) {
            throw new IllegalArgumentException("Current position quantity cannot be null");
        }

        List<PositionStep> history = getPositionHistory(accountId, symbol);

        boolean neverShort = history.stream()
            .allMatch(step -> step.quantityAfter().signum() >= 0);
        BigDecimal expectedQuantity = history.isEmpty()
            ? BigDecimal.ZERO
            : history.get(history.size() - 1).quantityAfter();

        return neverShort && expectedQuantity.compareTo(currentPositionQuantity) == 0;
    }

    public List<PositionStep> getPositionHistory(String accountId, String symbol) {
        List<PositionStep> steps = new ArrayList<>();
        BigDecimal held = BigDecimal.ZERO;
        // History comes back newest first; replaying needs oldest first
        List<ClientTrade> oldestFirst = new ArrayList<>(getSymbolHistory(accountId, symbol));
        Collections.reverse(oldestFirst);
        for (ClientTrade trade : oldestFirst) {
            held = trade.getTradeType() == OrderSide.BUY
                ? held.add(trade.getQuantity())
                : held.subtract(trade.getQuantity());
            steps.add(new PositionStep(trade, held));
        }
        return steps;
    }

    public ClientTrade saveTrade(ClientTrade trade) {
        if (trade == null) {
            throw new IllegalArgumentException("ClientTrade cannot be null");
        }
        return clientTradeRepository.save(trade);
    }

    public Optional<ClientTrade> getTradeById(Integer tradeId) {
        if (tradeId == null) {
            throw new IllegalArgumentException("TradeId cannot be null");
        }
        return clientTradeRepository.findById(tradeId);
    }

    @Transactional
    public boolean deleteTrade(Integer tradeId) {
        if (tradeId == null) {
            throw new IllegalArgumentException("TradeId cannot be null");
        }
        if (!clientTradeRepository.existsById(tradeId)) {
            return false;
        }
        clientTradeRepository.deleteById(tradeId);
        return true;
    }
}
