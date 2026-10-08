package com.solar.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "inspections")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "panel_id")
    private Panel panel;

    private LocalDate inspectionDate;
    private String filePath;
    private String status;
    private String defectType;
    private Double confidenceScore;
    private Double estimatedRepairCost;

    
    
    
    
    private String costCurrency;

    private String inspectionType;

    
    
    private String processingStatus;

    
    @ManyToOne
    @JoinColumn(name = "video_batch_id")
    private VideoInspectionBatch videoBatch;

    private Integer frameIndex;
    private Double frameTimestampSeconds;

    
    
    
    
    private String uploadedBy;
}