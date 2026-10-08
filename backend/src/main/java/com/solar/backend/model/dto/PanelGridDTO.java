package com.solar.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PanelGridDTO {
    private Long panelId;
    private Integer rowNumber;
    private Integer columnNumber;
    private String label;
    private String latestStatus;
    private String latestDefectType;
    private Double latestConfidenceScore;
    private Double latestEstimatedRepairCost;
    private String latestCostCurrency;
    private LocalDate lastInspectionDate;
    private Integer wattage;
    private String tier;
    private Integer effectiveWattage;
    private String effectiveTier;
}