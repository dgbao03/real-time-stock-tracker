package com.baodo.stocktracker.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SymbolTrackingRequest {
    private String currentSymbol;
    private String newSymbol;
}
