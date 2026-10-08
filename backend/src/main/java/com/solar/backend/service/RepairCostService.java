package com.solar.backend.service;

import com.solar.backend.model.Currency;
import com.solar.backend.model.DefectType;
import com.solar.backend.model.Farm;
import com.solar.backend.model.Panel;
import com.solar.backend.model.PanelTier;
import org.springframework.stereotype.Service;


@Service
public class RepairCostService {

    private final CurrencyService currencyService;

    public RepairCostService(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    private static final double DEFAULT_LABOR_RATE_USD = 100.0;
    private static final double SERVICE_CALL_MIN_USD = 100.0;

    
    
    private static final double CRACK_REPAIR_LOW_USD = 120.0;
    private static final double CRACK_REPAIR_HIGH_USD = 550.0;
    private static final double ELECTRICAL_FIX_LOW_USD = 100.0;
    private static final double ELECTRICAL_FIX_HIGH_USD = 300.0;
    private static final double SOILING_LIGHT_USD = 10.0;
    private static final double SOILING_MODERATE_USD = 15.0;
    private static final double SOILING_HEAVY_USD = 20.0;
    private static final double BIRD_DROPPINGS_USD = 22.0;

    
    
    public record CostEstimate(double amount, Currency currency) {
    }

    public CostEstimate estimateCost(String defectTypeRaw, Double confidenceScore, Panel panel) {
        DefectType defectType = DefectType.fromString(defectTypeRaw);
        double confidence = confidenceScore != null ? Math.max(0.0, Math.min(1.0, confidenceScore)) : 0.7;

        Farm farm = panel != null ? panel.getFarm() : null;
        Currency currency = (farm != null && farm.getCurrency() != null) ? farm.getCurrency() : Currency.USD;
        double laborRate = (farm != null && farm.getLaborRatePerHour() != null)
                ? farm.getLaborRatePerHour() : currencyService.fromUsd(DEFAULT_LABOR_RATE_USD, currency);
        double serviceCallMin = currencyService.fromUsd(SERVICE_CALL_MIN_USD, currency);
        double crackLow = currencyService.fromUsd(CRACK_REPAIR_LOW_USD, currency);
        double crackHigh = currencyService.fromUsd(CRACK_REPAIR_HIGH_USD, currency);
        double electricalLow = currencyService.fromUsd(ELECTRICAL_FIX_LOW_USD, currency);
        double electricalHigh = currencyService.fromUsd(ELECTRICAL_FIX_HIGH_USD, currency);
        double moduleCost = moduleCost(panel, currency);

        double cost = switch (defectType) {
            case CRACK ->
                
                
                
                
                confidence >= 0.85
                    ? moduleCost + laborRate * 1.2
                    : interpolate(crackLow, crackHigh, confidence);

            case DIODE_FAILURE ->
                
                
                
                interpolate(electricalLow, electricalHigh, confidence);

            case HOTSPOT ->
                
                
                
                
                
                
                confidence >= 0.9
                    ? moduleCost * 0.5 + serviceCallMin
                    : interpolate(electricalLow, electricalHigh, confidence);

            case PID_EFFECT ->
                
                
                
                
                
                
                confidence >= 0.8
                    ? moduleCost + laborRate
                    : serviceCallMin + electricalLow / 2.0;

            case OBSTRUCTION ->
                
                
                
                laborRate * 0.5;

            case SOILING_LIGHT -> currencyService.fromUsd(SOILING_LIGHT_USD, currency);
            case SOILING_MODERATE -> currencyService.fromUsd(SOILING_MODERATE_USD, currency);
            case SOILING_HEAVY -> currencyService.fromUsd(SOILING_HEAVY_USD, currency);

            case BIRD_DROPPINGS ->
                
                
                
                
                currencyService.fromUsd(BIRD_DROPPINGS_USD, currency);

            case NONE -> 0.0;
        };

        return new CostEstimate(round2(cost), currency);
    }

    private double moduleCost(Panel panel, Currency currency) {
        int wattage = panel != null ? panel.getEffectiveWattage() : 400;
        PanelTier tier = panel != null ? panel.getEffectiveTier() : PanelTier.STANDARD;
        return currencyService.fromUsd(wattage * tier.getPricePerWatt(), currency);
    }

    private double interpolate(double low, double high, double confidence) {
        return low + (high - low) * confidence;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
