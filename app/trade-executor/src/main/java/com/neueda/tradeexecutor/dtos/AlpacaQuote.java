package com.neueda.tradeexecutor.dtos;

import java.math.BigDecimal;
import java.time.Instant;

// One Alpaca quote: bp = bid price, ap = ask price, t = quote time (UTC)
public record AlpacaQuote(
    BigDecimal bp,
    BigDecimal ap,
    Instant t
) {}
