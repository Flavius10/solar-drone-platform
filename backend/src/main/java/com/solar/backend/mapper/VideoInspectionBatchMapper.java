package com.solar.backend.mapper;

import com.solar.backend.model.VideoInspectionBatch;
import com.solar.backend.model.dto.VideoInspectionBatchDTO;

public class VideoInspectionBatchMapper {

    public static VideoInspectionBatchDTO toDTO(VideoInspectionBatch batch) {
        if (batch == null) return null;

        VideoInspectionBatchDTO dto = new VideoInspectionBatchDTO();
        dto.setId(batch.getId());
        dto.setInspectionType(batch.getInspectionType());
        dto.setStatus(batch.getStatus());
        dto.setTotalFrames(batch.getTotalFrames());
        dto.setProcessedFrames(batch.getProcessedFrames());
        dto.setErrorMessage(batch.getErrorMessage());
        dto.setCreatedBy(batch.getCreatedBy());
        dto.setCreatedAt(batch.getCreatedAt());

        if (batch.getFarm() != null) {
            dto.setFarmId(batch.getFarm().getId());
            dto.setFarmName(batch.getFarm().getName());
        }

        return dto;
    }
}
