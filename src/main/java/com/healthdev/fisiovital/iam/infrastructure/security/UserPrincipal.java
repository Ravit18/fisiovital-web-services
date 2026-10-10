package com.healthdev.fisiovital.iam.infrastructure.security;

import com.healthdev.fisiovital.iam.domain.model.Role;

/** Datos del usuario autenticado, extraidos del token JWT. */
public record UserPrincipal(Long userId, String email, Role role) {
}
