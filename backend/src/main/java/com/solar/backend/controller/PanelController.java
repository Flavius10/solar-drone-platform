package com.solar.backend.controller;

import com.solar.backend.model.dto.BulkImportResultDTO;
import com.solar.backend.model.dto.PanelDTO;
import com.solar.backend.model.dto.PanelGridDTO;
import com.solar.backend.service.PanelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/panels")
@CrossOrigin(origins = "http://localhost:4200")
public class PanelController {

    private final PanelService panelService;

    public PanelController(PanelService panelService) {
        this.panelService = panelService;
    }

    @GetMapping("/grid")
    public ResponseEntity<?> getGrid(@RequestParam(value = "farmId", required = false) Long farmId) {
        try {
            return ResponseEntity.ok(panelService.getPanelGrid(farmId));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createPanel(@RequestBody PanelDTO request) {
        try {
            PanelDTO created = panelService.createPanel(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Error: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updateSpecs(@PathVariable Long id, @RequestBody PanelDTO request) {
        try {
            return ResponseEntity.ok(panelService.updatePanelSpecs(id, request));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> getHistory(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(panelService.getPanelHistory(id));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/bulk-import")
    public ResponseEntity<?> bulkImport(@RequestParam("file") MultipartFile file, @RequestParam("farmId") Long farmId) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Error: File is empty.");
            }
            BulkImportResultDTO result = panelService.bulkImportPanels(file, farmId);
            return ResponseEntity.ok(result);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error reading file: " + e.getMessage());
        }
    }
}
