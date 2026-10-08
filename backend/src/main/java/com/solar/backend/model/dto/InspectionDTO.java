package com.solar.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InspectionDTO {
    private Long id;
    private Long panelId;
    private Integer panelRow;
    private Integer panelColumn;
    private LocalDate inspectionDate;
    private String fileName;
    private String status;
    private String defectType;
    private Double confidenceScore;
    private Double estimatedRepairCost;
    private String costCurrency;
    private String inspectionType;
    private String processingStatus;
    private Long videoBatchId;
    private Integer frameIndex;
    private Double frameTimestampSeconds;
    private String uploadedBy;
}