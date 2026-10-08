package com.solar.backend.mapper;

import com.solar.backend.model.WorkOrder;
import com.solar.backend.model.dto.WorkOrderDTO;

public class WorkOrderMapper {

    public static WorkOrderDTO toDTO(WorkOrder workOrder) {
        if (workOrder == null) return null;

        WorkOrderDTO dto = new WorkOrderDTO();
        dto.setId(workOrder.getId());
        dto.setPanelId(workOrder.getPanel() != null ? workOrder.getPanel().getId() : null);
        dto.setPanelRow(workOrder.getPanel() != null ? workOrder.getPanel().getRowNumber() : null);
        dto.setPanelColumn(workOrder.getPanel() != null ? workOrder.getPanel().getColumnNumber() : null);
        if (workOrder.getPanel() != null && workOrder.getPanel().getFarm() != null) {
            dto.setFarmId(workOrder.getPanel().getFarm().getId());
            dto.setFarmName(workOrder.getPanel().getFarm().getName());
        }
        dto.setInspectionId(workOrder.getInspection() != null ? workOrder.getInspection().getId() : null);
        dto.setTitle(workOrder.getTitle());
        dto.setDescription(workOrder.getDescription());
        dto.setStatus(workOrder.getStatus());
        dto.setCreatedBy(workOrder.getCreatedBy());
        dto.setAssignedTo(workOrder.getAssignedTo());
        dto.setCreatedAt(workOrder.getCreatedAt());
        dto.setUpdatedAt(workOrder.getUpdatedAt());
        return dto;
    }
}
