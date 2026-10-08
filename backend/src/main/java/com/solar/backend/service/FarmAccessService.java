package com.solar.backend.service;

import com.solar.backend.model.Farm;
import com.solar.backend.model.UserAccount;
import com.solar.backend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;


@Service
public class FarmAccessService {

    private final UserRepository userRepository;

    public FarmAccessService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return auth.getName();
    }

    
    public Set<Long> getAccessibleFarmIds() {
        if (isAdmin()) {
            return null;
        }

        String username = getCurrentUsername();
        if (username == null) {
            return Set.of();
        }

        UserAccount user = userRepository.findByUsername(username);
        if (user == null) {
            return Set.of();
        }

        return user.getFarms().stream().map(Farm::getId).collect(Collectors.toSet());
    }

    public boolean hasAccess(Long farmId) {
        if (farmId == null) {
            return true;
        }
        Set<Long> accessible = getAccessibleFarmIds();
        return accessible == null || accessible.contains(farmId);
    }

    public void requireAccess(Long farmId) {
        if (!hasAccess(farmId)) {
            throw new AccessDeniedException("You don't have access to this farm.");
        }
    }
}
