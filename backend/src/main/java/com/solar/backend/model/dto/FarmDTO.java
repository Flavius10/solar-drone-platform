package com.solar.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FarmDTO {
    private Long id;
    private String name;
    private String location;
    private Integer defaultWattage;
    private String defaultTier;
    private Double criticalConfidenceThreshold;
    private Set<String> criticalDefectTypes;
    private String webhookUrl;
    private String currency;
    private Double laborRatePerHour;
    private String reportCompanyName;
    private String reportAccentColor;
    
    
    
    
    
    
    private Boolean hasReportLogo;
}
