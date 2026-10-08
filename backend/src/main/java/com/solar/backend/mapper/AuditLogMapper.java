package com.solar.backend.mapper;

import com.solar.backend.model.AuditLog;
import com.solar.backend.model.dto.AuditLogDTO;

public class AuditLogMapper {

    public static AuditLogDTO toDTO(AuditLog entity) {
        if (entity == null) return null;

        AuditLogDTO dto = new AuditLogDTO();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setAction(entity.getAction());
        dto.setDetails(entity.getDetails());
        dto.setTimestamp(entity.getTimestamp());
        return dto;
    }
}