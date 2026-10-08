package com.solar.backend.mapper;

import com.solar.backend.model.Notification;
import com.solar.backend.model.dto.NotificationDTO;

public class NotificationMapper {

    public static NotificationDTO toDTO(Notification notification) {
        if (notification == null) return null;

        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setPanelId(notification.getPanel() != null ? notification.getPanel().getId() : null);
        dto.setPanelRow(notification.getPanel() != null ? notification.getPanel().getRowNumber() : null);
        dto.setPanelColumn(notification.getPanel() != null ? notification.getPanel().getColumnNumber() : null);
        dto.setInspectionId(notification.getInspection() != null ? notification.getInspection().getId() : null);
        dto.setMessage(notification.getMessage());
        dto.setType(notification.getType());
        dto.setRead(notification.isRead());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }
}
