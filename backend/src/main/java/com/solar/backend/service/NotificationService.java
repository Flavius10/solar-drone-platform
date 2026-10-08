package com.solar.backend.service;

import com.solar.backend.mapper.NotificationMapper;
import com.solar.backend.model.Farm;
import com.solar.backend.model.Inspection;
import com.solar.backend.model.Notification;
import com.solar.backend.model.UserAccount;
import com.solar.backend.model.dto.NotificationDTO;
import com.solar.backend.repository.NotificationRepository;
import com.solar.backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    
    
    
    
    private static final Set<String> DEFAULT_CRITICAL_DEFECT_TYPES =
            Set.of("HOTSPOT", "CRACK", "PID_EFFECT", "DIODE_FAILURE");
    private static final double DEFAULT_CRITICAL_CONFIDENCE_THRESHOLD = 0.7;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final FarmAccessService farmAccessService;
    private final SseNotificationService sseNotificationService;
    private final WebhookService webhookService;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository,
                                EmailService emailService, FarmAccessService farmAccessService,
                                SseNotificationService sseNotificationService, WebhookService webhookService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.farmAccessService = farmAccessService;
        this.sseNotificationService = sseNotificationService;
        this.webhookService = webhookService;
    }

    public boolean isCriticalDefect(Inspection inspection) {
        if (!"DEFECT".equals(inspection.getStatus())) {
            return false;
        }
        if (inspection.getDefectType() == null) {
            return false;
        }

        Farm farm = (inspection.getPanel() != null) ? inspection.getPanel().getFarm() : null;
        Set<String> criticalTypes = (farm != null && farm.getCriticalDefectTypesCsv() != null)
                ? farm.getCriticalDefectTypes() : DEFAULT_CRITICAL_DEFECT_TYPES;
        double threshold = (farm != null && farm.getCriticalConfidenceThreshold() != null)
                ? farm.getCriticalConfidenceThreshold() : DEFAULT_CRITICAL_CONFIDENCE_THRESHOLD;

        if (!criticalTypes.contains(inspection.getDefectType().toUpperCase())) {
            return false;
        }
        return inspection.getConfidenceScore() != null && inspection.getConfidenceScore() >= threshold;
    }

    public void notifyCriticalDefect(Inspection inspection) {
        String panelLabel = "Row " + inspection.getPanel().getRowNumber() + ", Column " + inspection.getPanel().getColumnNumber();

        Notification notification = new Notification();
        notification.setPanel(inspection.getPanel());
        notification.setInspection(inspection);
        notification.setType("CRITICAL_DEFECT");
        notification.setMessage("Critical " + inspection.getDefectType() + " detected on panel (" + panelLabel + ") - "
                + Math.round(inspection.getConfidenceScore() * 100) + "% confidence.");
        notification.setRead(false);
        notificationRepository.save(notification);

        sseNotificationService.broadcast("notification", NotificationMapper.toDTO(notification), farmIdOf(notification));

        List<UserAccount> recipients = userRepository.findAll().stream()
                .filter(u -> "ADMIN".equals(u.getRole()) || "TECHNICIAN".equals(u.getRole()))
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .collect(Collectors.toList());

        for (UserAccount recipient : recipients) {
            try {
                emailService.sendCriticalDefectAlert(recipient.getEmail(), panelLabel,
                        inspection.getDefectType(), inspection.getConfidenceScore());
            } catch (Exception e) {
                
                System.err.println("Failed to send critical defect email to " + recipient.getEmail() + ": " + e.getMessage());
            }
        }

        Farm farm = (inspection.getPanel() != null) ? inspection.getPanel().getFarm() : null;
        if (farm != null && farm.getWebhookUrl() != null && !farm.getWebhookUrl().isBlank()) {
            try {
                webhookService.send(farm.getWebhookUrl(), ":rotating_light: Critical " + inspection.getDefectType()
                        + " detected on panel (" + panelLabel + ") - "
                        + Math.round(inspection.getConfidenceScore() * 100) + "% confidence.");
            } catch (Exception e) {
                
                System.err.println("Failed to send webhook alert for farm '" + farm.getName() + "': " + e.getMessage());
            }
        }
    }

    private Long farmIdOf(Notification n) {
        return (n.getPanel() != null && n.getPanel().getFarm() != null) ? n.getPanel().getFarm().getId() : null;
    }

    private List<Notification> getAccessibleNotifications() {
        java.util.Set<Long> accessible = farmAccessService.getAccessibleFarmIds();
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusHours(24);
        return notificationRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(n -> !n.isRead() || n.getCreatedAt().isAfter(cutoff))
                .filter(n -> accessible == null || farmIdOf(n) == null || accessible.contains(farmIdOf(n)))
                .collect(Collectors.toList());
    }

    public List<NotificationDTO> getAll() {
        return getAccessibleNotifications().stream()
                .map(NotificationMapper::toDTO)
                .collect(Collectors.toList());
    }

    public long getUnreadCount() {
        return getAccessibleNotifications().stream().filter(n -> !n.isRead()).count();
    }

    public NotificationDTO markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with id " + id));
        farmAccessService.requireAccess(farmIdOf(notification));
        notification.setRead(true);
        return NotificationMapper.toDTO(notificationRepository.save(notification));
    }

    public void delete(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with id " + id));
        farmAccessService.requireAccess(farmIdOf(notification));
        notificationRepository.deleteById(id);
    }
}
