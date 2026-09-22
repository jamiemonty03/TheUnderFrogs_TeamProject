package com.neueda.instrumentservice.controllers;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.neueda.instrumentservice.dtos.responses.BondResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.services.BondService;

@RestController
@RequestMapping("/bonds")
public class BondController {

    private final BondService bondService;

    public BondController(BondService bondService) {
        this.bondService = bondService;
    }

    @GetMapping
    public ResponseEntity<List<BondResponse>> getBonds() {
        return ResponseEntity.ok(bondService.getAllBonds());
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<BondResponse> getBond(@PathVariable String symbol) throws InstrumentNotFoundException {
        return ResponseEntity.ok(bondService.getBondBySymbol(symbol));
    }
}
