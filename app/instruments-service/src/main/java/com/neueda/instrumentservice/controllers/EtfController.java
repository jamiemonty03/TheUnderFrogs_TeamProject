package com.neueda.instrumentservice.controllers;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.neueda.instrumentservice.dtos.responses.EtfResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.services.EtfService;

@RestController
@RequestMapping("/etfs")
public class EtfController {

    private final EtfService etfService;

    public EtfController(EtfService etfService) {
        this.etfService = etfService;
    }

    @GetMapping
    public ResponseEntity<List<EtfResponse>> getEtfs() {
        return ResponseEntity.ok(etfService.getAllEtfs());
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<EtfResponse> getEtf(@PathVariable String symbol) throws InstrumentNotFoundException {
        return ResponseEntity.ok(etfService.getEtfBySymbol(symbol));
    }
}
