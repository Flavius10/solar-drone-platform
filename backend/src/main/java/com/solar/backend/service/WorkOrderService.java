package com.solar.backend.service;

import com.solar.backend.mapper.WorkOrderMapper;
import com.solar.backend.model.Inspection;
import com.solar.backend.model.Panel;
import com.solar.backend.model.WorkOrder;
import com.solar.backend.model.dto.AssignWorkOrderDTO;
import com.solar.backend.model.dto.CreateWorkOrderDTO;
import com.solar.backend.model.dto.UpdateWorkOrderStatusDTO;
import com.solar.backend.model.dto.WorkOrderDTO;
import com.solar.backend.repository.InspectionRepository;
import com.solar.backend.repository.PanelRepository;
import com.solar.backend.repository.WorkOrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class WorkOrderService {

    private static final List<String> STATUS_ORDER = List.of("OPEN", "ASSIGNED", "IN_PROGRESS", "RESOLVED", "CLOSED");
    private static final Set<String> VALID_STATUSES = Set.copyOf(STATUS_ORDER);

    private final WorkOrderRepository workOrderRepository;
    private final PanelRepository panelRepository;
    private final InspectionRepository inspectionRepository;
    private final AuditLogService auditLogService;
    private final FarmAccessService farmAccessService;

    public WorkOrderService(WorkOrderRepository workOrderRepository, PanelRepository panelRepository,
                             InspectionRepository inspectionRepository, AuditLogService auditLogService,
                             FarmAccessService farmAccessService) {
        this.workOrderRepository = workOrderRepository;
        this.panelRepository = panelRepository;
        this.inspectionRepository = inspectionRepository;
        this.auditLogService = auditLogService;
        this.farmAccessService = farmAccessService;
    }

    private Long farmIdOf(WorkOrder workOrder) {
        return (workOrder.getPanel() != null && workOrder.getPanel().getFarm() != null)
                ? workOrder.getPanel().getFarm().getId() : null;
    }

    public List<WorkOrderDTO> getAll() {
        java.util.Set<Long> accessible = farmAccessService.getAccessibleFarmIds();
        return workOrderRepository.findAll().stream()
                .filter(w -> accessible == null || farmIdOf(w) == null || accessible.contains(farmIdOf(w)))
                .map(WorkOrderMapper::toDTO)
                .collect(Collectors.toList());
    }

    public WorkOrderDTO getById(Long id) {
        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Work order not found with id " + id));
        farmAccessService.requireAccess(farmIdOf(workOrder));
        return WorkOrderMapper.toDTO(workOrder);
    }

    public List<WorkOrderDTO> getByPanel(Long panelId) {
        Panel panel = panelRepository.findById(panelId)
                .orElseThrow(() -> new IllegalArgumentException("Panel not found with id " + panelId));
        farmAccessService.requireAccess(panel.getFarm() != null ? panel.getFarm().getId() : null);
        return workOrderRepository.findByPanel(panel).stream()
                .map(WorkOrderMapper::toDTO)
                .collect(Collectors.toList());
    }

    public WorkOrderDTO createWorkOrder(CreateWorkOrderDTO request) {
        if (request.getPanelId() == null) {
            throw new IllegalArgumentException("panelId is required.");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new IllegalArgumentException("title is required.");
        }

        Panel panel = panelRepository.findById(request.getPanelId())
                .orElseThrow(() -> new IllegalArgumentException("Panel not found with id " + request.getPanelId()));

        farmAccessService.requireAccess(panel.getFarm() != null ? panel.getFarm().getId() : null);

        Inspection inspection = null;
        if (request.getInspectionId() != null) {
            inspection = inspectionRepository.findById(request.getInspectionId())
                    .orElseThrow(() -> new IllegalArgumentException("Inspection not found with id " + request.getInspectionId()));
        }

        WorkOrder workOrder = new WorkOrder();
        workOrder.setPanel(panel);
        workOrder.setInspection(inspection);
        workOrder.setTitle(request.getTitle());
        workOrder.setDescription(request.getDescription());
        workOrder.setStatus("OPEN");
        workOrder.setCreatedBy(auditLogService.getCurrentUsername());

        WorkOrder saved = workOrderRepository.save(workOrder);
        auditLogService.logCurrentUser("CREATE_WORK_ORDER",
                "Work order #" + saved.getId() + " created for panel (row " + panel.getRowNumber()
                        + ", col " + panel.getColumnNumber() + "): " + saved.getTitle());
        return WorkOrderMapper.toDTO(saved);
    }

    public WorkOrderDTO assign(Long id, AssignWorkOrderDTO request) {
        if (request.getAssignedTo() == null || request.getAssignedTo().isBlank()) {
            throw new IllegalArgumentException("assignedTo is required.");
        }

        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Work order not found with id " + id));
        farmAccessService.requireAccess(farmIdOf(workOrder));

        if ("CLOSED".equals(workOrder.getStatus())) {
            throw new IllegalArgumentException("Cannot assign a work order that is already CLOSED.");
        }

        workOrder.setAssignedTo(request.getAssignedTo());
        if ("OPEN".equals(workOrder.getStatus())) {
            workOrder.setStatus("ASSIGNED");
        }

        WorkOrder saved = workOrderRepository.save(workOrder);
        auditLogService.logCurrentUser("ASSIGN_WORK_ORDER",
                "Work order #" + saved.getId() + " assigned to " + saved.getAssignedTo());
        return WorkOrderMapper.toDTO(saved);
    }

    public WorkOrderDTO updateStatus(Long id, UpdateWorkOrderStatusDTO request) {
        String newStatus = request.getStatus() != null ? request.getStatus().toUpperCase() : null;
        if (newStatus == null || !VALID_STATUSES.contains(newStatus)) {
            throw new IllegalArgumentException("Invalid status. Must be one of " + STATUS_ORDER);
        }

        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Work order not found with id " + id));
        farmAccessService.requireAccess(farmIdOf(workOrder));

        if ("CLOSED".equals(workOrder.getStatus())) {
            throw new IllegalArgumentException("Cannot change status of a CLOSED work order.");
        }

        String previousStatus = workOrder.getStatus();
        workOrder.setStatus(newStatus);
        WorkOrder saved = workOrderRepository.save(workOrder);

        auditLogService.logCurrentUser("UPDATE_WORK_ORDER_STATUS",
                "Work order #" + saved.getId() + " status changed " + previousStatus + " -> " + newStatus);
        return WorkOrderMapper.toDTO(saved);
    }
}
