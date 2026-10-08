package com.solar.backend.controller;

import com.solar.backend.model.dto.DashboardStatsDTO;
import com.solar.backend.model.dto.InspectionDTO;
import com.solar.backend.model.dto.VideoInspectionBatchDTO;
import com.solar.backend.service.InspectionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/inspections")
@CrossOrigin(origins = "http://localhost:4200")
public class InspectionController {

    private final InspectionService inspectionService;

    public InspectionController(InspectionService inspectionService) {
        this.inspectionService = inspectionService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file,
                                          @RequestParam("panelId") Long panelId,
                                          @RequestParam(value = "inspectionType", required = false) String inspectionType) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Error: File is empty.");
            }
            InspectionDTO created = inspectionService.uploadImage(file, panelId, inspectionType);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing file: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(inspectionService.getById(id));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/upload-video")
    public ResponseEntity<?> uploadVideo(@RequestParam("file") MultipartFile file,
                                          @RequestParam("farmId") Long farmId,
                                          @RequestParam(value = "inspectionType", required = false) String inspectionType) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Error: File is empty.");
            }
            VideoInspectionBatchDTO batch = inspectionService.uploadVideo(file, farmId, inspectionType);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(batch);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing video: " + e.getMessage());
        }
    }

    @GetMapping("/video-batches")
    public ResponseEntity<?> getVideoBatches(@RequestParam(value = "farmId", required = false) Long farmId) {
        try {
            return ResponseEntity.ok(inspectionService.getVideoBatches(farmId));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/video-batches/{id}")
    public ResponseEntity<?> getVideoBatch(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(inspectionService.getVideoBatch(id));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/video-batches/{id}/frames")
    public ResponseEntity<?> getVideoBatchFrames(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(inspectionService.getVideoBatchFrames(id));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@RequestParam(value = "farmId", required = false) Long farmId) {
        try {
            return ResponseEntity.ok(inspectionService.getDashboardStats(farmId));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@RequestParam(value = "farmId", required = false) Long farmId) {
        try {
            return ResponseEntity.ok(inspectionService.getAllReports(farmId));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error: " + e.getMessage());
        }
    }
}
