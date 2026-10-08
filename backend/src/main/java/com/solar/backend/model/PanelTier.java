package com.solar.backend.model;


public enum PanelTier {
    BUDGET(0.30),
    STANDARD(0.55),
    PREMIUM(0.95);

    private final double pricePerWatt;

    PanelTier(double pricePerWatt) {
        this.pricePerWatt = pricePerWatt;
    }

    public double getPricePerWatt() {
        return pricePerWatt;
    }

    public static PanelTier fromString(String value) {
        if (value == null) return STANDARD;
        try {
            return PanelTier.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return STANDARD;
        }
    }
}
