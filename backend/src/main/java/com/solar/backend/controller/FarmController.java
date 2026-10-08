package com.solar.backend.controller;

import com.solar.backend.model.dto.AssignUserToFarmDTO;
import com.solar.backend.model.dto.FarmDTO;
import com.solar.backend.service.FarmService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URLConnection;
import java.util.List;

@RestController
@RequestMapping("/api/farms")
@CrossOrigin(origins = "http://localhost:4200")
public class FarmController {

    private final FarmService farmService;

    public FarmController(FarmService farmService) {
        this.farmService = farmService;
    }

    @GetMapping
    public ResponseEntity<List<FarmDTO>> getAll() {
        return ResponseEntity.ok(farmService.getAll());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody FarmDTO request) {
        try {
            FarmDTO created = farmService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/defaults")
    public ResponseEntity<?> updateDefaults(@PathVariable Long id, @RequestBody FarmDTO request) {
        try {
            return ResponseEntity.ok(farmService.updateDefaults(id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/notification-settings")
    public ResponseEntity<?> updateNotificationSettings(@PathVariable Long id, @RequestBody FarmDTO request) {
        try {
            return ResponseEntity.ok(farmService.updateNotificationSettings(id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/pricing-settings")
    public ResponseEntity<?> updatePricingSettings(@PathVariable Long id, @RequestBody FarmDTO request) {
        try {
            return ResponseEntity.ok(farmService.updatePricingSettings(id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/test-webhook")
    public ResponseEntity<?> testWebhook(@PathVariable Long id) {
        try {
            farmService.testWebhook(id);
            return ResponseEntity.ok("Test webhook sent.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Error: Failed to reach webhook URL - " + e.getMessage());
        }
    }

    @PutMapping("/{id}/branding-settings")
    public ResponseEntity<?> updateBrandingSettings(@PathVariable Long id, @RequestBody FarmDTO request) {
        try {
            return ResponseEntity.ok(farmService.updateBrandingSettings(id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @PostMapping(value = "/{id}/branding-logo", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadLogo(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(farmService.uploadLogo(id, file));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: Failed to save logo - " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/branding-logo")
    public ResponseEntity<?> removeLogo(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(farmService.removeLogo(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/branding-logo")
    public ResponseEntity<?> getLogo(@PathVariable Long id) {
        try {
            File file = farmService.getLogoFile(id);
            String contentType = URLConnection.guessContentTypeFromName(file.getName());
            return ResponseEntity.ok()
                    .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM)
                    .body(new FileSystemResource(file));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/users")
    public ResponseEntity<?> getUsers(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(farmService.getUsersForFarm(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/users")
    public ResponseEntity<?> assignUser(@PathVariable Long id, @RequestBody AssignUserToFarmDTO request) {
        try {
            farmService.assignUser(id, request.getUsername());
            return ResponseEntity.ok("User assigned to farm.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/users/{username}")
    public ResponseEntity<?> unassignUser(@PathVariable Long id, @PathVariable String username) {
        try {
            farmService.unassignUser(id, username);
            return ResponseEntity.ok("User unassigned from farm.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage());
        }
    }
}
