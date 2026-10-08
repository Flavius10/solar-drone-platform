package com.solar.backend.service;

import com.solar.backend.mapper.InspectionMapper;
import com.solar.backend.mapper.PanelMapper;
import com.solar.backend.model.Farm;
import com.solar.backend.model.Inspection;
import com.solar.backend.model.Panel;
import com.solar.backend.model.PanelTier;
import com.solar.backend.model.dto.BulkImportResultDTO;
import com.solar.backend.model.dto.InspectionDTO;
import com.solar.backend.model.dto.PanelDTO;
import com.solar.backend.model.dto.PanelGridDTO;
import com.solar.backend.repository.FarmRepository;
import com.solar.backend.repository.InspectionRepository;
import com.solar.backend.repository.PanelRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PanelService {

    private final PanelRepository panelRepository;
    private final InspectionRepository inspectionRepository;
    private final FarmRepository farmRepository;
    private final AuditLogService auditLogService;
    private final FarmAccessService farmAccessService;

    public PanelService(PanelRepository panelRepository, InspectionRepository inspectionRepository,
                        FarmRepository farmRepository, AuditLogService auditLogService,
                        FarmAccessService farmAccessService) {
        this.panelRepository = panelRepository;
        this.inspectionRepository = inspectionRepository;
        this.farmRepository = farmRepository;
        this.auditLogService = auditLogService;
        this.farmAccessService = farmAccessService;
    }

    public List<PanelGridDTO> getPanelGrid(Long farmId) {
        List<Panel> panels;
        if (farmId != null) {
            farmAccessService.requireAccess(farmId);
            panels = panelRepository.findByFarmId(farmId);
        } else {
            java.util.Set<Long> accessible = farmAccessService.getAccessibleFarmIds();
            panels = panelRepository.findAll().stream()
                    .filter(p -> accessible == null || p.getFarm() == null || accessible.contains(p.getFarm().getId()))
                    .collect(Collectors.toList());
        }

        return panels.stream()
                .map(panel -> {
                    Optional<Inspection> latest = inspectionRepository.findTopByPanelOrderByIdDesc(panel);
                    return PanelMapper.toGridDTO(panel, latest.orElse(null));
                })
                .collect(Collectors.toList());
    }

    public List<InspectionDTO> getPanelHistory(Long panelId) {
        Panel panel = panelRepository.findById(panelId)
                .orElseThrow(() -> new IllegalArgumentException("Panel not found with id " + panelId));

        farmAccessService.requireAccess(panel.getFarm() != null ? panel.getFarm().getId() : null);

        return inspectionRepository.findByPanelOrderByInspectionDateAscIdAsc(panel).stream()
                .map(InspectionMapper::toDTO)
                .collect(Collectors.toList());
    }

    public PanelDTO createPanel(PanelDTO request) {
        if (request.getFarmId() == null) {
            throw new IllegalArgumentException("farmId is required.");
        }
        farmAccessService.requireAccess(request.getFarmId());

        Farm farm = farmRepository.findById(request.getFarmId())
                .orElseThrow(() -> new IllegalArgumentException("Farm not found with id " + request.getFarmId()));

        Panel existing = panelRepository.findByFarmAndRowNumberAndColumnNumber(farm, request.getRowNumber(), request.getColumnNumber());
        if (existing != null) {
            throw new IllegalArgumentException(
                    "A panel already exists at row " + request.getRowNumber() + ", column " + request.getColumnNumber() + " for this farm.");
        }

        Panel panel = new Panel();
        panel.setFarm(farm);
        panel.setRowNumber(request.getRowNumber());
        panel.setColumnNumber(request.getColumnNumber());
        panel.setLabel(request.getLabel());
        panel.setWattage(request.getWattage());
        panel.setTier(request.getTier() != null ? PanelTier.fromString(request.getTier()) : null);

        Panel saved = panelRepository.save(panel);
        auditLogService.logCurrentUser("CREATE_PANEL",
                "Farm '" + farm.getName() + "', Row " + panel.getRowNumber() + ", Column " + panel.getColumnNumber()
                        + (panel.getLabel() != null ? ", Label " + panel.getLabel() : ""));
        return PanelMapper.toDTO(saved);
    }

    
    
    
    
    public PanelDTO updatePanelSpecs(Long panelId, PanelDTO request) {
        Panel panel = panelRepository.findById(panelId)
                .orElseThrow(() -> new IllegalArgumentException("Panel not found with id " + panelId));
        farmAccessService.requireAccess(panel.getFarm() != null ? panel.getFarm().getId() : null);

        panel.setWattage(request.getWattage());
        panel.setTier(request.getTier() != null ? PanelTier.fromString(request.getTier()) : null);

        Panel saved = panelRepository.save(panel);
        auditLogService.logCurrentUser("UPDATE_PANEL_SPECS",
                "Panel " + panel.getRowNumber() + "-" + panel.getColumnNumber()
                        + " -> wattage=" + request.getWattage() + ", tier=" + request.getTier());
        return PanelMapper.toDTO(saved);
    }

    public BulkImportResultDTO bulkImportPanels(MultipartFile file, Long farmId) throws IOException {
        farmAccessService.requireAccess(farmId);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new IllegalArgumentException("Farm not found with id " + farmId));

        int created = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            int lineNumber = 0;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) continue;

                if (isFirstLine) {
                    isFirstLine = false;
                    if (line.toLowerCase().startsWith("row")) {
                        continue;
                    }
                }

                String[] parts = line.split(",");
                if (parts.length < 2) {
                    errors.add("Line " + lineNumber + ": expected at least 'row,column', got '" + line + "'");
                    continue;
                }

                try {
                    int row = Integer.parseInt(parts[0].trim());
                    int column = Integer.parseInt(parts[1].trim());
                    String label = parts.length > 2 && !parts[2].trim().isEmpty() ? parts[2].trim() : null;
                    Integer wattage = parts.length > 3 && !parts[3].trim().isEmpty() ? Integer.parseInt(parts[3].trim()) : null;
                    PanelTier tier = parts.length > 4 && !parts[4].trim().isEmpty() ? PanelTier.fromString(parts[4].trim()) : null;

                    Panel existing = panelRepository.findByFarmAndRowNumberAndColumnNumber(farm, row, column);
                    if (existing != null) {
                        skipped++;
                        continue;
                    }

                    Panel panel = new Panel();
                    panel.setFarm(farm);
                    panel.setRowNumber(row);
                    panel.setColumnNumber(column);
                    panel.setLabel(label);
                    panel.setWattage(wattage);
                    panel.setTier(tier);
                    panelRepository.save(panel);
                    created++;
                } catch (NumberFormatException e) {
                    errors.add("Line " + lineNumber + ": invalid row/column number in '" + line + "'");
                }
            }
        }

        auditLogService.logCurrentUser("BULK_IMPORT_PANELS",
                "Created " + created + " panels, skipped " + skipped + " duplicates, " + errors.size() + " errors");

        return new BulkImportResultDTO(created, skipped, errors);
    }
}