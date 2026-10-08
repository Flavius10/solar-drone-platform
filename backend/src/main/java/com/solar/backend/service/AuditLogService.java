package com.solar.backend.service;

import com.solar.backend.mapper.AuditLogMapper;
import com.solar.backend.model.AuditLog;
import com.solar.backend.model.dto.AuditLogDTO;
import com.solar.backend.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String username, String action, String details) {
        AuditLog entry = new AuditLog();
        entry.setUsername(username);
        entry.setAction(action);
        entry.setDetails(details);
        entry.setTimestamp(LocalDateTime.now());
        auditLogRepository.save(entry);
    }

    public void logCurrentUser(String action, String details) {
        log(getCurrentUsername(), action, details);
    }

    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "SYSTEM";
        }
        return auth.getName();
    }

    public Page<AuditLogDTO> getLogsPage(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return auditLogRepository.findAllByOrderByTimestampDesc(pageRequest)
                .map(AuditLogMapper::toDTO);
    }
}