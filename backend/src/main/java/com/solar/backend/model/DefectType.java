package com.solar.backend.model;


public enum DefectType {
    CRACK,
    DIODE_FAILURE,
    HOTSPOT,
    PID_EFFECT,
    OBSTRUCTION,
    SOILING_LIGHT,
    SOILING_MODERATE,
    SOILING_HEAVY,
    BIRD_DROPPINGS,
    NONE;

    public static DefectType fromString(String value) {
        if (value == null) return NONE;
        try {
            return DefectType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
