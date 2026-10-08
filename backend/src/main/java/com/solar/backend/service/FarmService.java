package com.solar.backend.service;

import com.solar.backend.mapper.FarmMapper;
import com.solar.backend.model.Currency;
import com.solar.backend.model.Farm;
import com.solar.backend.model.PanelTier;
import com.solar.backend.model.UserAccount;
import com.solar.backend.model.dto.FarmDTO;
import com.solar.backend.model.dto.UserSummaryDTO;
import com.solar.backend.repository.FarmRepository;
import com.solar.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FarmService {

    private final FarmRepository farmRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final FarmAccessService farmAccessService;
    private final WebhookService webhookService;

    @Value("${file.logo-upload-dir}")
    private String logoUploadDir;

    public FarmService(FarmRepository farmRepository, UserRepository userRepository, AuditLogService auditLogService,
                        FarmAccessService farmAccessService, WebhookService webhookService) {
        this.farmRepository = farmRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.farmAccessService = farmAccessService;
        this.webhookService = webhookService;
    }

    public List<FarmDTO> getAll() {
        java.util.Set<Long> accessible = farmAccessService.getAccessibleFarmIds();
        return farmRepository.findAll().stream()
                .filter(f -> accessible == null || accessible.contains(f.getId()))
                .map(FarmMapper::toDTO)
                .collect(Collectors.toList());
    }

    public FarmDTO create(FarmDTO request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Farm name is required.");
        }
        if (farmRepository.findByName(request.getName()) != null) {
            throw new IllegalArgumentException("A farm with this name already exists.");
        }

        Farm farm = new Farm();
        farm.setName(request.getName());
        farm.setLocation(request.getLocation());

        Farm saved = farmRepository.save(farm);
        auditLogService.logCurrentUser("CREATE_FARM", "Created farm '" + saved.getName() + "'");
        return FarmMapper.toDTO(saved);
    }

    
    
    public FarmDTO updateDefaults(Long farmId, FarmDTO request) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new IllegalArgumentException("Farm not found with id " + farmId));

        if (request.getDefaultWattage() != null) {
            farm.setDefaultWattage(request.getDefaultWattage());
        }
        if (request.getDefaultTier() != null) {
            farm.setDefaultTier(PanelTier.fromString(request.getDefaultTier()));
        }

        Farm saved = farmRepository.save(farm);
        auditLogService.logCurrentUser("UPDATE_FARM_DEFAULTS",
                "Farm '" + saved.getName() + "' -> " + saved.getDefaultWattage() + "W " + saved.getDefaultTier());
        return FarmMapper.toDTO(saved);
    }

    
    
    public FarmDTO updateNotificationSettings(Long farmId, FarmDTO request) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new IllegalArgumentException("Farm not found with id " + farmId));

        if (request.getCriticalConfidenceThreshold() != null) {
            double threshold = Math.max(0.0, Math.min(1.0, request.getCriticalConfidenceThreshold()));
            farm.setCriticalConfidenceThreshold(threshold);
        }
        if (request.getCriticalDefectTypes() != null) {
            farm.setCriticalDefectTypes(request.getCriticalDefectTypes());
        }
        if (request.getWebhookUrl() != null) {
            farm.setWebhookUrl(request.getWebhookUrl().isBlank() ? null : request.getWebhookUrl().trim());
        }

        Farm saved = farmRepository.save(farm);
        auditLogService.logCurrentUser("UPDATE_FARM_NOTIFICATION_SETTINGS",
                "Farm '" + saved.getName() + "' -> threshold=" + saved.getCriticalConfidenceThreshold()
                        + ", types=" + saved.getCriticalDefectTypesCsv()
                        + ", webhook=" + (saved.getWebhookUrl() != null ? "configured" : "none"));
        return FarmMapper.toDTO(saved);
    }

    
    
    public FarmDTO updatePricingSettings(Long farmId, FarmDTO request) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new IllegalArgumentException("Farm not found with id " + farmId));

        if (request.getCurrency() != null) {
            farm.setCurrency(Currency.fromString(request.getCurrency()));
        }
        if (request.getLaborRatePerHour() != null) {
            farm.setLaborRatePerHour(Math.max(0.0, request.getLaborRatePerHour()));
        }

        Farm saved = farmRepository.save(farm);
        auditLogService.logCurrentUser("UPDATE_FARM_PRICING_SETTINGS",
                "Farm '" + saved.getName() + "' -> currency=" + saved.getCurrency()
                        + ", laborRate=" + saved.getLaborRatePerHour());
        return FarmMapper.toDTO(saved);
    }

    
    public void testWebhook(Long farmId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new IllegalArgumentException("Farm not found with id " + farmId));
        if (farm.getWebhookUrl() == null || farm.getWebhookUrl().isBlank()) {
            throw new IllegalArgumentException("No webhook URL configured for this farm.");
        }
        webhookService.send(farm.getWebhookUrl(),
                "Test alert from Axela - webhook is configured correctly for farm '" + farm.getName() + "'.");
    }

    
    
    
    public FarmDTO updateBrandingSettings(Long farmId, FarmDTO request) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new IllegalArgumentException("Farm not found with id " + farmId));

        if (request.getReportCompanyName() != null) {
            farm.setReportCompanyName(request.getReportCompanyName().isBlank() ? null : request.getReportCompanyName().trim());
        }
        if (request.getReportAccentColor() != null) {
            String color = request.getReportAccentColor().trim();
            if (color.isBlank()) {
                farm.setReportAccentColor(null);
            } else if (!color.matches("^#[0-9a-fA-F]{6}$")) {
                throw new IllegalArgumentException("Accent color must be a hex value like #1a2b3c.");
            } else {
                farm.setReportAccentColor(color);
            }
        }

        Farm saved = farmRepository.save(farm);
        auditLogService.logCurrentUser("UPDATE_FARM_BRANDING_SETTINGS",
                "Farm '" + saved.getName() + "' -> companyName=" + saved.getReportCompanyName()
                        + ", accentColor=" + saved.getReportAccentColor());
        return FarmMapper.toDTO(saved);
    }

    public FarmDTO uploadLogo(Long farmId, MultipartFile file) throws IOException {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new IllegalArgumentException("Farm not found with id " + farmId));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file uploaded.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Logo must be an image file (PNG, JPG, etc.).");
        }
        
        
        
        
        if (contentType.equals("image/svg+xml")) {
            throw new IllegalArgumentException("SVG logos aren't supported in PDF reports - please upload a PNG or JPG instead.");
        }

        File dir = new File(logoUploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        
        
        deleteLogoFileIfExists(farm);

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "logo";
        String safeName = "farm" + farmId + "_" + System.currentTimeMillis() + "_" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path filePath = new File(dir, safeName).toPath();
        Files.write(filePath, file.getBytes());

        farm.setReportLogoPath(filePath.toString());
        Farm saved = farmRepository.save(farm);
        auditLogService.logCurrentUser("UPLOAD_FARM_LOGO", "Farm '" + saved.getName() + "' -> uploaded report logo");
        return FarmMapper.toDTO(saved);
    }

    public FarmDTO removeLogo(Long farmId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new IllegalArgumentException("Farm not found with id " + farmId));

        deleteLogoFileIfExists(farm);
        farm.setReportLogoPath(null);
        Farm saved = farmRepository.save(farm);
        auditLogService.logCurrentUser("REMOVE_FARM_LOGO", "Farm '" + saved.getName() + "' -> removed report logo");
        return FarmMapper.toDTO(saved);
    }

    private void deleteLogoFileIfExists(Farm farm) {
        if (farm.getReportLogoPath() == null) return;
        try {
            Files.deleteIfExists(new File(farm.getReportLogoPath()).toPath());
        } catch (IOException e) {
            
            System.err.println("Failed to delete old logo file for farm '" + farm.getName() + "': " + e.getMessage());
        }
    }

    
    
    public File getLogoFile(Long farmId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new IllegalArgumentException("Farm not found with id " + farmId));
        if (farm.getReportLogoPath() == null) {
            throw new IllegalArgumentException("This farm has no logo uploaded.");
        }
        File file = new File(farm.getReportLogoPath());
        if (!file.exists()) {
            throw new IllegalArgumentException("Logo file is missing on disk.");
        }
        return file;
    }

    public List<UserSummaryDTO> getUsersForFarm(Long farmId) {
        if (!farmRepository.existsById(farmId)) {
            throw new IllegalArgumentException("Farm not found with id " + farmId);
        }

        return userRepository.findByFarms_Id(farmId).stream()
                .map(u -> new UserSummaryDTO(u.getId(), u.getUsername(), u.getRole(), u.getSubscriptionTier(), u.getEmail()))
                .collect(Collectors.toList());
    }

    public void assignUser(Long farmId, String username) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new IllegalArgumentException("Farm not found with id " + farmId));
        UserAccount user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + username);
        }

        user.getFarms().add(farm);
        userRepository.save(user);

        auditLogService.logCurrentUser("ASSIGN_USER_TO_FARM", "Assigned user '" + username + "' to farm '" + farm.getName() + "'");
    }

    public void unassignUser(Long farmId, String username) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new IllegalArgumentException("Farm not found with id " + farmId));
        UserAccount user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + username);
        }

        user.getFarms().remove(farm);
        userRepository.save(user);

        auditLogService.logCurrentUser("UNASSIGN_USER_FROM_FARM", "Unassigned user '" + username + "' from farm '" + farm.getName() + "'");
    }
}
