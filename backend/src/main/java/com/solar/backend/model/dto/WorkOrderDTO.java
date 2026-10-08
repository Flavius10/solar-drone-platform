package com.solar.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkOrderDTO {
    private Long id;
    private Long panelId;
    private Long farmId;
    private String farmName;
    private Integer panelRow;
    private Integer panelColumn;
    private Long inspectionId;
    private String title;
    private String description;
    private String status;
    private String createdBy;
    private String assignedTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
