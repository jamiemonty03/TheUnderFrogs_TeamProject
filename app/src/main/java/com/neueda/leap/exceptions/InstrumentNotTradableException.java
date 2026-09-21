package com.neueda.leap.exceptions;


public class InstrumentNotTradableException extends TradingException {
    

    public InstrumentNotTradableException(String symbol) {
        super(String.format(
            "Instrument '%s' is not tradable. Verify the Instrument.tradable flag in master data.",
            symbol
        ));
    }
}
