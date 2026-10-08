package com.solar.backend.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkOrderDTO {
    private Long panelId;
    private Long inspectionId;
    private String title;
    private String description;
}
