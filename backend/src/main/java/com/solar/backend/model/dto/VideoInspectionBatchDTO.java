package com.solar.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoInspectionBatchDTO {
    private Long id;
    private Long farmId;
    private String farmName;
    private String inspectionType;
    private String status;
    private Integer totalFrames;
    private Integer processedFrames;
    private String errorMessage;
    private String createdBy;
    private LocalDateTime createdAt;
}
