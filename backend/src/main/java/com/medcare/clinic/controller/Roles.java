package com.medcare.clinic.controller;

import org.springframework.security.core.userdetails.UserDetails;

final class Roles {
    private Roles() {}

    static boolean laKeToan(UserDetails user) {
        return user.getAuthorities().stream().anyMatch(a -> "ROLE_KE_TOAN".equals(a.getAuthority()));
    }
}
