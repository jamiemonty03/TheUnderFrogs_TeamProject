package com.neueda.positionservice.models;

import java.io.Serializable;
import java.util.Objects;

public record PositionId(String accountId, String symbol) implements Serializable {}