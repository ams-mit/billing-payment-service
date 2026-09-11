package com.ams.billing.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

public class SecurityUtils {

    /**
     * Extracts raw roles (without ROLE_ prefix) from the authentication object.
     * e.g. ROLE_FINANCE_OFFICER -> FINANCE_OFFICER
     */
    public static List<String> extractRoles(Authentication auth) {
        if (auth == null) return List.of();

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                .toList();
    }
}
