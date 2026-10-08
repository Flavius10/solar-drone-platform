package com.solar.backend.service;

import com.solar.backend.model.dto.DashboardStatsDTO;
import com.solar.backend.model.dto.InspectionDTO;
import com.solar.backend.model.dto.VideoInspectionBatchDTO;
import com.solar.backend.mapper.InspectionMapper;
import com.solar.backend.mapper.VideoInspectionBatchMapper;
import com.solar.backend.model.Farm;
import com.solar.backend.model.Inspection;
import com.solar.backend.model.Panel;
import com.solar.backend.model.VideoInspectionBatch;
import com.solar.backend.repository.FarmRepository;
import com.solar.backend.repository.InspectionRepository;
import com.solar.backend.repository.PanelRepository;
import com.solar.backend.repository.VideoInspectionBatchRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InspectionService {

    private final InspectionRepository repository;
    private final PanelRepository panelRepository;
    private final FarmRepository farmRepository;
    private final VideoInspectionBatchRepository videoBatchRepository;
    private final AuditLogService auditLogService;
    private final FarmAccessService farmAccessService;
    private final InspectionProcessingService processingService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.video-upload-dir}")
    private String videoUploadDir;

    public InspectionService(InspectionRepository repository, PanelRepository panelRepository,
                              FarmRepository farmRepository, VideoInspectionBatchRepository videoBatchRepository,
                              AuditLogService auditLogService, FarmAccessService farmAccessService,
                              InspectionProcessingService processingService) {
        this.repository = repository;
        this.panelRepository = panelRepository;
        this.farmRepository = farmRepository;
        this.videoBatchRepository = videoBatchRepository;
        this.auditLogService = auditLogService;
        this.farmAccessService = farmAccessService;
        this.processingService = processingService;
    }

    private List<Inspection> filterAccessible(List<Inspection> inspections) {
        java.util.Set<Long> accessible = farmAccessService.getAccessibleFarmIds();
        if (accessible == null) return inspections;
        return inspections.stream()
                .filter(i -> i.getPanel() == null || i.getPanel().getFarm() == null
                        || accessible.contains(i.getPanel().getFarm().getId()))
                .collect(Collectors.toList());
    }

    private List<VideoInspectionBatch> filterAccessibleBatches(List<VideoInspectionBatch> batches) {
        java.util.Set<Long> accessible = farmAccessService.getAccessibleFarmIds();
        if (accessible == null) return batches;
        return batches.stream()
                .filter(b -> b.getFarm() == null || accessible.contains(b.getFarm().getId()))
                .collect(Collectors.toList());
    }

    private Long farmIdOf(VideoInspectionBatch batch) {
        return batch.getFarm() != null ? batch.getFarm().getId() : null;
    }

    public DashboardStatsDTO getDashboardStats(Long farmId) {
        List<Inspection> inspections;
        String currency = "USD";
        if (farmId != null) {
            farmAccessService.requireAccess(farmId);
            inspections = repository.findByPanel_FarmId(farmId);
            Farm farm = farmRepository.findById(farmId).orElse(null);
            if (farm != null && farm.getCurrency() != null) {
                currency = farm.getCurrency().name();
            }
        } else {
            
            
            
            
            inspections = filterAccessible(repository.findAll());
        }

        long total = inspections.size();
        long defects = inspections.stream().filter(i -> "DEFECT".equals(i.getStatus())).count();
        long healthy = inspections.stream().filter(i -> "OK".equals(i.getStatus())).count();
        double totalCost = inspections.stream()
                .mapToDouble(i -> i.getEstimatedRepairCost() != null ? i.getEstimatedRepairCost() : 0.0)
                .sum();

        return new DashboardStatsDTO(total, defects, healthy, totalCost, currency);
    }

    public List<InspectionDTO> getAllReports(Long farmId) {
        List<Inspection> inspections;
        if (farmId != null) {
            farmAccessService.requireAccess(farmId);
            inspections = repository.findByPanel_FarmId(farmId);
        } else {
            inspections = filterAccessible(repository.findAll());
        }
        return inspections.stream()
                .map(InspectionMapper::toDTO)
                .collect(Collectors.toList());
    }

    public InspectionDTO getById(Long id) {
        Inspection inspection = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inspection not found with id: " + id));
        farmAccessService.requireAccess(inspection.getPanel() != null && inspection.getPanel().getFarm() != null
                ? inspection.getPanel().getFarm().getId() : null);
        return InspectionMapper.toDTO(inspection);
    }

    
    public InspectionDTO uploadImage(MultipartFile file, Long panelId, String inspectionType) throws IOException {
        Panel panel = panelRepository.findById(panelId)
                .orElseThrow(() -> new IllegalArgumentException("Panel not found with id: " + panelId));

        farmAccessService.requireAccess(panel.getFarm() != null ? panel.getFarm().getId() : null);

        String type = (inspectionType != null && !inspectionType.isBlank()) ? inspectionType.toUpperCase() : "RGB";

        if (type.equals("THERMAL") && !callerHasThermalAccess()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Thermal inspections require a PRO or ENTERPRISE subscription.");
        }

        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir, fileName);
        Files.write(filePath, file.getBytes());

        
        
        
        String username = auditLogService.getCurrentUsername();

        Inspection inspection = new Inspection();
        inspection.setPanel(panel);
        inspection.setInspectionType(type);
        inspection.setInspectionDate(LocalDate.now());
        inspection.setFilePath(filePath.toString());
        inspection.setProcessingStatus("PENDING");
        inspection.setUploadedBy(username);
        repository.save(inspection);
        processingService.processInspectionAsync(inspection.getId(), username);

        return InspectionMapper.toDTO(inspection);
    }

    
    public VideoInspectionBatchDTO uploadVideo(MultipartFile file, Long farmId, String inspectionType) throws IOException {
        if (farmId == null) {
            throw new IllegalArgumentException("farmId is required.");
        }
        farmAccessService.requireAccess(farmId);

        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new IllegalArgumentException("Farm not found with id: " + farmId));

        if (panelRepository.findByFarmId(farmId).isEmpty()) {
            throw new IllegalArgumentException("This farm has no panels yet - add panels before uploading a video.");
        }

        String type = (inspectionType != null && !inspectionType.isBlank()) ? inspectionType.toUpperCase() : "RGB";

        if (type.equals("THERMAL") && !callerHasThermalAccess()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Thermal inspections require a PRO or ENTERPRISE subscription.");
        }

        File directory = new File(videoUploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(videoUploadDir, fileName);
        Files.write(filePath, file.getBytes());

        VideoInspectionBatch batch = new VideoInspectionBatch();
        batch.setFarm(farm);
        batch.setInspectionType(type);
        batch.setVideoFilePath(filePath.toString());
        batch.setStatus("PENDING");
        batch.setProcessedFrames(0);
        batch.setCreatedAt(LocalDateTime.now());
        batch.setCreatedBy(auditLogService.getCurrentUsername());
        videoBatchRepository.save(batch);

        processingService.processVideoBatchAsync(batch.getId());

        return VideoInspectionBatchMapper.toDTO(batch);
    }

    public List<VideoInspectionBatchDTO> getVideoBatches(Long farmId) {
        List<VideoInspectionBatch> batches;
        if (farmId != null) {
            farmAccessService.requireAccess(farmId);
            batches = videoBatchRepository.findByFarm_IdOrderByCreatedAtDesc(farmId);
        } else {
            batches = filterAccessibleBatches(videoBatchRepository.findAllByOrderByCreatedAtDesc());
        }
        return batches.stream().map(VideoInspectionBatchMapper::toDTO).collect(Collectors.toList());
    }

    public VideoInspectionBatchDTO getVideoBatch(Long id) {
        VideoInspectionBatch batch = videoBatchRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Video batch not found with id: " + id));
        farmAccessService.requireAccess(farmIdOf(batch));
        return VideoInspectionBatchMapper.toDTO(batch);
    }

    public List<InspectionDTO> getVideoBatchFrames(Long batchId) {
        VideoInspectionBatch batch = videoBatchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Video batch not found with id: " + batchId));
        farmAccessService.requireAccess(farmIdOf(batch));
        return repository.findByVideoBatchIdOrderByFrameIndexAsc(batchId).stream()
                .map(InspectionMapper::toDTO)
                .collect(Collectors.toList());
    }

    private boolean callerHasThermalAccess() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof com.solar.backend.security.CustomUserDetails userDetails)) {
            return false;
        }
        String tier = userDetails.getSubscriptionTier();
        return "PRO".equalsIgnoreCase(tier) || "ENTERPRISE".equalsIgnoreCase(tier);
    }
}
