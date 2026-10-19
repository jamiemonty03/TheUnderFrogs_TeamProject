package com.neueda.tradeexecutor.events;

/** In-process event raised by MarketDataPoller each time it publishes a new price for a symbol. */
public record PriceChanged(String symbol) {}
