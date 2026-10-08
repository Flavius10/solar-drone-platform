package com.solar.backend.model;


public enum Currency {
    USD("$"),
    EUR("€"),
    RON("RON");

    private final String symbol;

    Currency(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public static Currency fromString(String value) {
        if (value == null) return USD;
        try {
            return Currency.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return USD;
        }
    }
}
