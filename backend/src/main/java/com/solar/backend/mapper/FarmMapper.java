package com.solar.backend.mapper;

import com.solar.backend.model.Farm;
import com.solar.backend.model.dto.FarmDTO;

public class FarmMapper {

    public static FarmDTO toDTO(Farm farm) {
        if (farm == null) return null;

        FarmDTO dto = new FarmDTO();
        dto.setId(farm.getId());
        dto.setName(farm.getName());
        dto.setLocation(farm.getLocation());
        dto.setDefaultWattage(farm.getDefaultWattage());
        dto.setDefaultTier(farm.getDefaultTier() != null ? farm.getDefaultTier().name() : null);
        dto.setCriticalConfidenceThreshold(farm.getCriticalConfidenceThreshold());
        dto.setCriticalDefectTypes(farm.getCriticalDefectTypes());
        dto.setWebhookUrl(farm.getWebhookUrl());
        dto.setCurrency(farm.getCurrency() != null ? farm.getCurrency().name() : "USD");
        dto.setLaborRatePerHour(farm.getLaborRatePerHour());
        dto.setReportCompanyName(farm.getReportCompanyName());
        dto.setReportAccentColor(farm.getReportAccentColor());
        dto.setHasReportLogo(farm.getReportLogoPath() != null && !farm.getReportLogoPath().isBlank());
        return dto;
    }
}
