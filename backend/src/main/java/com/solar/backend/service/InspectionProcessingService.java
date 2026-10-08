package com.solar.backend.service;

import com.solar.backend.mapper.InspectionMapper;
import com.solar.backend.mapper.VideoInspectionBatchMapper;
import com.solar.backend.model.DefectType;
import com.solar.backend.model.Inspection;
import com.solar.backend.model.Panel;
import com.solar.backend.model.VideoInspectionBatch;
import com.solar.backend.repository.InspectionRepository;
import com.solar.backend.repository.PanelRepository;
import com.solar.backend.repository.VideoInspectionBatchRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class InspectionProcessingService {

    private final InspectionRepository inspectionRepository;
    private final VideoInspectionBatchRepository videoBatchRepository;
    private final PanelRepository panelRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final SseNotificationService sseNotificationService;
    private final RepairCostService repairCostService;
    private final SoilingDetectionService soilingDetectionService;
    private final DefectDetectionService defectDetectionService;

    @Value("${video.scene-change-threshold:0.3}")
    private double sceneChangeThreshold;

    @Value("${ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    private static final int MAX_FRAMES_PER_VIDEO = 300;

    public InspectionProcessingService(InspectionRepository inspectionRepository,
                                       VideoInspectionBatchRepository videoBatchRepository,
                                       PanelRepository panelRepository,
                                       AuditLogService auditLogService,
                                       NotificationService notificationService,
                                       SseNotificationService sseNotificationService,
                                       RepairCostService repairCostService,
                                       SoilingDetectionService soilingDetectionService,
                                       DefectDetectionService defectDetectionService) {
        this.inspectionRepository = inspectionRepository;
        this.videoBatchRepository = videoBatchRepository;
        this.panelRepository = panelRepository;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
        this.sseNotificationService = sseNotificationService;
        this.repairCostService = repairCostService;
        this.soilingDetectionService = soilingDetectionService;
        this.defectDetectionService = defectDetectionService;
    }

    @Async("taskExecutor")
    public void processInspectionAsync(Long inspectionId, String username) {
        Inspection inspection = inspectionRepository.findById(inspectionId).orElse(null);
        if (inspection == null) return;

        try {
            inspection.setProcessingStatus("PROCESSING");
            inspectionRepository.save(inspection);
            broadcastInspection(inspection, "INSPECTION_STATUS");

            simulateProcessingDelay();

            DetectionResult result = simulateDetection(inspection.getPanel(), inspection.getFilePath());
            inspection.setStatus(result.status());
            inspection.setDefectType(result.defectType());
            inspection.setConfidenceScore(result.confidence());
            inspection.setEstimatedRepairCost(result.cost());
            inspection.setCostCurrency(result.currency());
            inspection.setProcessingStatus("COMPLETE");
            inspectionRepository.save(inspection);

            auditLogService.log(username, "UPLOAD_INSPECTION",
                    "Panel " + inspection.getPanel().getRowNumber() + "-" + inspection.getPanel().getColumnNumber()
                            + ", type " + inspection.getInspectionType() + ", status " + result.status());

            if (notificationService.isCriticalDefect(inspection)) {
                notificationService.notifyCriticalDefect(inspection);
            }

            broadcastInspection(inspection, "INSPECTION_COMPLETE");
        } catch (Exception e) {
            inspection.setProcessingStatus("FAILED");
            inspectionRepository.save(inspection);
            broadcastInspection(inspection, "INSPECTION_FAILED");
        }
    }

    @Async("taskExecutor")
    public void processVideoBatchAsync(Long batchId) {
        VideoInspectionBatch batch = videoBatchRepository.findById(batchId).orElse(null);
        if (batch == null) return;

        try {
            batch.setStatus("EXTRACTING");
            videoBatchRepository.save(batch);
            broadcastBatch(batch, "VIDEO_BATCH_STATUS");

            Path framesDir = Paths.get(new File(batch.getVideoFilePath()).getParent(), "frames", "batch_" + batch.getId());
            Files.createDirectories(framesDir);

            String filterExpr = "select=eq(n\\,0)+gt(scene\\," + sceneChangeThreshold + "),showinfo";
            List<String> cmd = List.of(ffmpegPath, "-y", "-i", batch.getVideoFilePath(),
                    "-vf", filterExpr, "-fps_mode", "vfr", "-q:v", "3",
                    framesDir.resolve("frame_%04d.jpg").toString());

            Process process;
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);
                process = pb.start();
            } catch (IOException e) {
                fail(batch, "ffmpeg nu a fost gasit (cale folosita: '" + ffmpegPath + "'). Verifica ca e instalat, sau seteaza calea absoluta in application.properties (ffmpeg.path=...).");
                return;
            }

            String output = new String(process.getInputStream().readAllBytes());
            boolean finished = process.waitFor(5, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                fail(batch, "Extragerea cadrelor din video a durat prea mult si a fost anulata.");
                return;
            }
            if (process.exitValue() != 0) {
                String tail = output.length() > 400 ? output.substring(output.length() - 400) : output;
                fail(batch, "ffmpeg a esuat la extragerea cadrelor: " + tail);
                return;
            }

            File[] frameFiles = framesDir.toFile().listFiles((dir, name) -> name.toLowerCase().endsWith(".jpg"));
            if (frameFiles == null || frameFiles.length == 0) {
                fail(batch, "Nu s-a putut extrage niciun cadru din videoul incarcat.");
                return;
            }
            Arrays.sort(frameFiles, Comparator.comparing(File::getName));

            if (frameFiles.length > MAX_FRAMES_PER_VIDEO) {
                frameFiles = Arrays.copyOf(frameFiles, MAX_FRAMES_PER_VIDEO);
            }

            List<Double> frameTimestamps = parseShowinfoTimestamps(output);

            List<Panel> farmPanels = panelRepository.findByFarmId(batch.getFarm().getId()).stream()
                    .sorted(Comparator.comparing(Panel::getRowNumber).thenComparing(Panel::getColumnNumber))
                    .toList();
            if (farmPanels.isEmpty()) {
                fail(batch, "Ferma nu mai are panouri - videoul nu poate fi asociat cu niciun panou.");
                return;
            }

            batch.setTotalFrames(frameFiles.length);
            batch.setStatus("PROCESSING");
            videoBatchRepository.save(batch);
            broadcastBatch(batch, "VIDEO_BATCH_PROGRESS");

            for (int i = 0; i < frameFiles.length; i++) {
                Panel assignedPanel = farmPanels.get(i % farmPanels.size());
                DetectionResult result = simulateDetection(assignedPanel, frameFiles[i].getPath());

                Inspection frameInspection = new Inspection();
                frameInspection.setPanel(assignedPanel);
                frameInspection.setInspectionType(batch.getInspectionType());
                frameInspection.setInspectionDate(LocalDate.now());
                frameInspection.setFilePath(frameFiles[i].getPath());
                frameInspection.setVideoBatch(batch);
                frameInspection.setUploadedBy(batch.getCreatedBy());
                frameInspection.setFrameIndex(i);
                frameInspection.setFrameTimestampSeconds(i < frameTimestamps.size() ? frameTimestamps.get(i) : null);
                frameInspection.setStatus(result.status());
                frameInspection.setDefectType(result.defectType());
                frameInspection.setConfidenceScore(result.confidence());
                frameInspection.setEstimatedRepairCost(result.cost());
                frameInspection.setCostCurrency(result.currency());
                frameInspection.setProcessingStatus("COMPLETE");
                inspectionRepository.save(frameInspection);

                if (notificationService.isCriticalDefect(frameInspection)) {
                    notificationService.notifyCriticalDefect(frameInspection);
                }

                batch.setProcessedFrames(i + 1);
                videoBatchRepository.save(batch);
                broadcastBatch(batch, "VIDEO_BATCH_PROGRESS");

                simulateProcessingDelay();
            }

            auditLogService.log(batch.getCreatedBy(), "UPLOAD_VIDEO_INSPECTION",
                    "Farm '" + batch.getFarm().getName() + "', " + frameFiles.length
                            + " cadre procesate pe " + farmPanels.size() + " panouri");

            batch.setStatus("COMPLETE");
            videoBatchRepository.save(batch);
            broadcastBatch(batch, "VIDEO_BATCH_COMPLETE");
        } catch (Exception e) {
            fail(batch, "Eroare neasteptata la procesarea videoului: " + e.getMessage());
        }
    }

    
    
    
    private static final Pattern PTS_TIME_PATTERN = Pattern.compile("pts_time:([0-9]+\\.?[0-9]*)");

    private List<Double> parseShowinfoTimestamps(String ffmpegOutput) {
        List<Double> timestamps = new ArrayList<>();
        Matcher matcher = PTS_TIME_PATTERN.matcher(ffmpegOutput);
        while (matcher.find()) {
            try {
                timestamps.add(Double.parseDouble(matcher.group(1)));
            } catch (NumberFormatException ignored) {
                
            }
        }
        return timestamps;
    }

    private void fail(VideoInspectionBatch batch, String message) {
        batch.setStatus("FAILED");
        batch.setErrorMessage(message);
        videoBatchRepository.save(batch);
        broadcastBatch(batch, "VIDEO_BATCH_FAILED");
    }

    private void broadcastInspection(Inspection inspection, String eventName) {
        Long farmId = inspection.getPanel() != null && inspection.getPanel().getFarm() != null
                ? inspection.getPanel().getFarm().getId() : null;
        sseNotificationService.broadcast(eventName, InspectionMapper.toDTO(inspection), farmId);
    }

    private void broadcastBatch(VideoInspectionBatch batch, String eventName) {
        Long farmId = batch.getFarm() != null ? batch.getFarm().getId() : null;
        sseNotificationService.broadcast(eventName, VideoInspectionBatchMapper.toDTO(batch), farmId);
    }

    private void simulateProcessingDelay() {
        try {
            Thread.sleep(800 + (long) (Math.random() * 1200));
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    private static final DefectType[] SIMULATED_DEFECT_TYPES = {
            DefectType.CRACK, DefectType.DIODE_FAILURE, DefectType.HOTSPOT, DefectType.PID_EFFECT,
            DefectType.OBSTRUCTION, DefectType.SOILING_LIGHT, DefectType.SOILING_MODERATE,
            DefectType.SOILING_HEAVY, DefectType.BIRD_DROPPINGS
    };

    private DetectionResult simulateDetection(Panel panel, String imagePath) {
        SoilingDetectionService.SoilingResult soiling = soilingDetectionService.detect(imagePath);
        if (soiling != null && !"CLEAN".equals(soiling.status())) {
            DefectType defectType = DefectType.fromString(soiling.status());
            double confidence = soiling.confidence();
            RepairCostService.CostEstimate estimate = repairCostService.estimateCost(defectType.name(), confidence, panel);
            return new DetectionResult("DEFECT", defectType.name(), round2(confidence), estimate.amount(), estimate.currency().name());
        }

        DefectDetectionService.DefectResult crackResult = defectDetectionService.detect(imagePath);
        if (crackResult != null && "CRACK".equals(crackResult.status())) {
            double confidence = crackResult.confidence();
            RepairCostService.CostEstimate estimate = repairCostService.estimateCost(DefectType.CRACK.name(), confidence, panel);
            return new DetectionResult("DEFECT", DefectType.CRACK.name(), round2(confidence), estimate.amount(), estimate.currency().name());
        }

        String status = Math.random() > 0.5 ? "OK" : "DEFECT";
        DefectType defectType = status.equals("DEFECT")
                ? SIMULATED_DEFECT_TYPES[(int) (Math.random() * SIMULATED_DEFECT_TYPES.length)]
                : DefectType.NONE;
        double confidence = status.equals("DEFECT") ? (0.6 + Math.random() * 0.4) : (0.85 + Math.random() * 0.15);
        RepairCostService.CostEstimate estimate = repairCostService.estimateCost(defectType.name(), confidence, panel);
        return new DetectionResult(status, defectType.name(), round2(confidence), estimate.amount(), estimate.currency().name());
    }
    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record DetectionResult(String status, String defectType, double confidence, double cost, String currency) {
    }
}