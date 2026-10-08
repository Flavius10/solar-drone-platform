package com.solar.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private Long panelId;
    private Integer panelRow;
    private Integer panelColumn;
    private Long inspectionId;
    private String message;
    private String type;
    private boolean read;
    private LocalDateTime createdAt;
}
