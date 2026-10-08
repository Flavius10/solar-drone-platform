package com.solar.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private long totalInspections;
    private long defectsFound;
    private long healthyPanels;
    private double totalEstimatedRepairCost;
    private String currency;
}