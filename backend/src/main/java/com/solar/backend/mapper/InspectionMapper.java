package com.solar.backend.mapper;

import com.solar.backend.model.dto.InspectionDTO;
import com.solar.backend.model.Inspection;
import java.nio.file.Paths;

public class InspectionMapper {

    public static InspectionDTO toDTO(Inspection entity) {
        if (entity == null) return null;

        InspectionDTO dto = new InspectionDTO();
        dto.setId(entity.getId());
        dto.setInspectionDate(entity.getInspectionDate());
        dto.setStatus(entity.getStatus());
        dto.setDefectType(entity.getDefectType());
        dto.setConfidenceScore(entity.getConfidenceScore());
        dto.setEstimatedRepairCost(entity.getEstimatedRepairCost());
        dto.setCostCurrency(entity.getCostCurrency() != null ? entity.getCostCurrency() : "USD");
        dto.setInspectionType(entity.getInspectionType());
        dto.setProcessingStatus(entity.getProcessingStatus());
        dto.setFrameIndex(entity.getFrameIndex());
        dto.setFrameTimestampSeconds(entity.getFrameTimestampSeconds());
        dto.setUploadedBy(entity.getUploadedBy());
        if (entity.getVideoBatch() != null) {
            dto.setVideoBatchId(entity.getVideoBatch().getId());
        }

        if (entity.getPanel() != null) {
            dto.setPanelId(entity.getPanel().getId());
            dto.setPanelRow(entity.getPanel().getRowNumber());
            dto.setPanelColumn(entity.getPanel().getColumnNumber());
        }

        if (entity.getFilePath() != null) {
            String fileName = Paths.get(entity.getFilePath()).getFileName().toString();
            dto.setFileName(fileName);
        }

        return dto;
    }
}