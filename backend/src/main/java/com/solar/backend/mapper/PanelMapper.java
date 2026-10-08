package com.solar.backend.mapper;

import com.solar.backend.model.Inspection;
import com.solar.backend.model.Panel;
import com.solar.backend.model.dto.PanelDTO;
import com.solar.backend.model.dto.PanelGridDTO;

public class PanelMapper {

    public static PanelDTO toDTO(Panel panel) {
        if (panel == null) return null;

        PanelDTO dto = new PanelDTO();
        dto.setId(panel.getId());
        dto.setFarmId(panel.getFarm() != null ? panel.getFarm().getId() : null);
        dto.setFarmName(panel.getFarm() != null ? panel.getFarm().getName() : null);
        dto.setRowNumber(panel.getRowNumber());
        dto.setColumnNumber(panel.getColumnNumber());
        dto.setLabel(panel.getLabel());
        dto.setWattage(panel.getWattage());
        dto.setTier(panel.getTier() != null ? panel.getTier().name() : null);
        dto.setEffectiveWattage(panel.getEffectiveWattage());
        dto.setEffectiveTier(panel.getEffectiveTier().name());
        return dto;
    }

    public static PanelGridDTO toGridDTO(Panel panel, Inspection latestInspection) {
        PanelGridDTO dto = new PanelGridDTO();
        dto.setPanelId(panel.getId());
        dto.setRowNumber(panel.getRowNumber());
        dto.setColumnNumber(panel.getColumnNumber());
        dto.setLabel(panel.getLabel());
        dto.setWattage(panel.getWattage());
        dto.setTier(panel.getTier() != null ? panel.getTier().name() : null);
        dto.setEffectiveWattage(panel.getEffectiveWattage());
        dto.setEffectiveTier(panel.getEffectiveTier().name());

        if (latestInspection != null) {
            boolean stillProcessing = latestInspection.getStatus() == null
                    && ("PENDING".equals(latestInspection.getProcessingStatus()) || "PROCESSING".equals(latestInspection.getProcessingStatus()));
            dto.setLatestStatus(stillProcessing ? "PROCESSING" : latestInspection.getStatus());
            dto.setLatestDefectType(latestInspection.getDefectType());
            dto.setLatestConfidenceScore(latestInspection.getConfidenceScore());
            dto.setLatestEstimatedRepairCost(latestInspection.getEstimatedRepairCost());
            dto.setLatestCostCurrency(latestInspection.getCostCurrency() != null ? latestInspection.getCostCurrency() : "USD");
            dto.setLastInspectionDate(latestInspection.getInspectionDate());
        } else {
            dto.setLatestStatus("NO_DATA");
        }

        return dto;
    }
}