package com.neueda.orderservice.controllers;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.neueda.orderservice.dtos.responses.ClientTradeResponse;
import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.exceptions.ClientTradeNotFoundException;
import com.neueda.orderservice.exceptions.InstrumentServiceUnavailableException;
import com.neueda.orderservice.models.ClientTrade;
import com.neueda.orderservice.models.ClientTradeFilter;
import com.neueda.orderservice.services.ClientTradeService;
import com.neueda.orderservice.services.InstrumentDirectory;

@RestController
@RequestMapping("/client-trades")
public class ClientTradeController {

    private static final Logger log = LoggerFactory.getLogger(ClientTradeController.class);

    private final ClientTradeService clientTradeService;
    private final InstrumentDirectory instrumentDirectory;

    public ClientTradeController(ClientTradeService clientTradeService, InstrumentDirectory instrumentDirectory) {
        this.clientTradeService = clientTradeService;
        this.instrumentDirectory = instrumentDirectory;
    }

    @GetMapping
    public ResponseEntity<List<ClientTradeResponse>> searchTrades(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String assetClass,
            @RequestParam(required = false) OrderSide side) throws InstrumentServiceUnavailableException {
        log.info("Trade history search received {} {} {} {} {} {}", kv("accountId", accountId), kv("from", from),
            kv("to", to), kv("symbol", symbol), kv("assetClass", assetClass), kv("side", side));

        Set<String> symbols = null;
        if (assetClass != null) {
            symbols = instrumentDirectory.symbolsInAssetClass(assetClass);
        }
        if (symbol != null) {
            symbols = symbols == null || symbols.contains(symbol) ? Set.of(symbol) : Set.of();
        }

        List<ClientTrade> trades = clientTradeService.search(new ClientTradeFilter(accountId, from, to, symbols, side));
        return ResponseEntity.ok(trades.stream().map(this::toClientTradeResponse).toList());
    }

    @GetMapping("/{tradeId}")
    public ResponseEntity<ClientTradeResponse> getTradeById(@PathVariable Integer tradeId)
            throws ClientTradeNotFoundException {
        ClientTrade trade = clientTradeService.getTradeById(tradeId)
            .orElseThrow(() -> new ClientTradeNotFoundException("Client trade not found: " + tradeId));
        return ResponseEntity.ok(toClientTradeResponse(trade));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<ClientTradeResponse>> getTradesByAccountId(@PathVariable String accountId) {
        List<ClientTradeResponse> responses = clientTradeService.getAccountHistory(accountId).stream()
            .map(this::toClientTradeResponse)
            .toList();
        return ResponseEntity.ok(responses);
    }

    private ClientTradeResponse toClientTradeResponse(ClientTrade trade) {
        return new ClientTradeResponse(
            trade.getTradeId(),
            trade.getAccountId(),
            trade.getSymbol(),
            trade.getTradeType(),
            trade.getQuantity(),
            trade.getPrice(),
            trade.getTradeDate(),
            trade.getVersion(),
            trade.getCreatedAt(),
            trade.getLastUpdated(),
            trade.getUpdatedBy()
        );
    }
}
