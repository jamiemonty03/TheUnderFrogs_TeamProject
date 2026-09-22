package com.neueda.instrumentservice.controllers;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.neueda.instrumentservice.dtos.responses.InstrumentResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.services.InstrumentService;

@RestController
@RequestMapping("/instruments")
public class InstrumentController {

    private final InstrumentService instrumentService;

    public InstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    @GetMapping
    public ResponseEntity<List<InstrumentResponse>> getInstruments() {
        return ResponseEntity.ok(instrumentService.getAllInstruments());
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<InstrumentResponse> getInstrument(@PathVariable String symbol)
            throws InstrumentNotFoundException {
        return ResponseEntity.ok(instrumentService.getInstrumentBySymbol(symbol));
    }
}
