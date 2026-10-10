package com.healthdev.fisiovital.iam.application;

import com.healthdev.fisiovital.iam.infrastructure.security.UserPrincipal;
import com.healthdev.fisiovital.shared.domain.exceptions.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public UserPrincipal get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw BusinessException.unauthorized("auth.unauthorized");
        }
        return principal;
    }
}
