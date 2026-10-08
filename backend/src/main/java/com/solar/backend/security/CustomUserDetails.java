package com.solar.backend.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class CustomUserDetails extends User {

    private final String subscriptionTier;

    public CustomUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities, String subscriptionTier) {
        super(username, password, authorities);
        this.subscriptionTier = subscriptionTier;
    }

    public String getSubscriptionTier() {
        return subscriptionTier;
    }
}