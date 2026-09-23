package com.neueda.instrumentservice.controllers;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.neueda.instrumentservice.dtos.responses.InstrumentResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.services.InstrumentService;

@RestController
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

    @DeleteMapping("/{symbol}")
    public ResponseEntity<Void> deleteInstrument(@PathVariable String symbol) throws InstrumentNotFoundException {
        instrumentService.deleteInstrument(symbol);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
