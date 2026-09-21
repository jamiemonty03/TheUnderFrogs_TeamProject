
package com.neueda.instrumentservice.controllers;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/instruments")
    public class InstrumentController {
    @GetMapping
    public String getInstruments() {
        return "List of instruments";
    }
}
