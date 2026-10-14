package com.neueda.instrumentservice.controllers;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.neueda.instrumentservice.dtos.responses.InstrumentResponse;
import com.neueda.instrumentservice.dtos.responses.LivePriceResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.services.InstrumentService;
import com.neueda.instrumentservice.services.LivePriceCache;

@RestController
@RequestMapping("/instruments")
public class InstrumentController {

    private final InstrumentService instrumentService;
    private final LivePriceCache livePriceCache;

    public InstrumentController(InstrumentService instrumentService, LivePriceCache livePriceCache) {
        this.instrumentService = instrumentService;
        this.livePriceCache = livePriceCache;
    }

    @GetMapping
    public ResponseEntity<List<InstrumentResponse>> getInstruments() {
        return ResponseEntity.ok(instrumentService.getAllInstruments());
    }

    @GetMapping("/live-prices")
    public ResponseEntity<List<LivePriceResponse>> getLivePrices(
            @RequestParam(required = false) List<String> symbols) {
        return ResponseEntity.ok(symbols == null || symbols.isEmpty()
                ? livePriceCache.getAll()
                : livePriceCache.get(symbols));
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<InstrumentResponse> getInstrument(@PathVariable String symbol)
            throws InstrumentNotFoundException {
        return ResponseEntity.ok(instrumentService.getInstrumentBySymbol(symbol));
    }

    @DeleteMapping("/{symbol}")
    public ResponseEntity<Void> deleteInstrument(@PathVariable String symbol) throws InstrumentNotFoundException {
        instrumentService.deleteInstrument(symbol);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
