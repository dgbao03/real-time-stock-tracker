package com.baodo.stocktracker.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SymbolQuoteResponse {
    // Current price
    private double c;

    // High price of the day
    private double h;

    // Low price of the day
    private double l;

    // Open price of the day
    private double o;

    // Previous close price
    private double pc;

    // Change
    private double d;

    // Percent change
    private double dp;

    // Timestamp
    private long t;
}
