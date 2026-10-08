package com.solar.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PanelDTO {
    private Long id;
    private Long farmId;
    private String farmName;
    private Integer rowNumber;
    private Integer columnNumber;
    private String label;
    private Integer wattage;
    private String tier;
    private Integer effectiveWattage;
    private String effectiveTier;
}