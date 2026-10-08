package com.solar.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "video_inspection_batches")
@Data
@NoArgsConstructor
public class VideoInspectionBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    
    
    
    @ManyToOne
    @JoinColumn(name = "farm_id")
    private Farm farm;

    private String inspectionType;
    private String videoFilePath;

    
    private String status;

    private Integer totalFrames;
    private Integer processedFrames;

    
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private String createdBy;
    private LocalDateTime createdAt;
}
