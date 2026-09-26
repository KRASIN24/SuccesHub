package com.succeshub.coreinfra.auth;

import com.succeshub.appdomain.repository.UserProfileRepository;
import com.succeshub.coreinfra.exception_handler.domain.AccountDeactivatedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Blocks most {@code /api/**} calls when the user's profile is in the deletion grace period.
 * Allows account status and reactivate so the SPA can show the confirm-first screen.
 */
@Component
@RequiredArgsConstructor
public class DeactivatedAccountInterceptor implements HandlerInterceptor {

    private final UserProfileRepository userProfileRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            return true;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();
        if (isAllowedWhileDeactivated(method, path)) {
            return true;
        }

        boolean deactivated = userProfileRepository.findByKeycloakId(oidcUser.getSubject())
                .map(profile -> profile.getDeactivatedAt() != null)
                .orElse(false);
        if (deactivated) {
            throw new AccountDeactivatedException();
        }
        return true;
    }

    private static boolean isAllowedWhileDeactivated(String method, String path) {
        if ("GET".equalsIgnoreCase(method) && "/api/account".equals(path)) {
            return true;
        }
        return "POST".equalsIgnoreCase(method) && "/api/account/reactivate".equals(path);
    }
}
