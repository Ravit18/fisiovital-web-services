package com.healthdev.fisiovital.security;

import com.healthdev.fisiovital.model.Role;

/** Datos del usuario autenticado, extraidos del token JWT. */
public record UserPrincipal(Long userId, String email, Role role) {
}
