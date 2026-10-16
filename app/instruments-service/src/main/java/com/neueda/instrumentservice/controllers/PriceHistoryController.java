package com.neueda.instrumentservice.controllers;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.neueda.instrumentservice.dtos.responses.PricePointResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.services.PriceHistoryService;

@RestController
@RequestMapping("/instruments")
public class PriceHistoryController {

    private final PriceHistoryService priceHistoryService;

    public PriceHistoryController(PriceHistoryService priceHistoryService) {
        this.priceHistoryService = priceHistoryService;
    }

    @GetMapping("/{symbol}/history")
    public ResponseEntity<List<PricePointResponse>> getHistory(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "" + PriceHistoryService.DEFAULT_DAYS) int days)
            throws InstrumentNotFoundException {
        return ResponseEntity.ok(priceHistoryService.getHistory(symbol, days));
    }
}
