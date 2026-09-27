package com.succeshub.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Persists SPA theme/locale bridge prefs as cookies on the OAuth entry response.
 *
 * <p>Keycloak strips custom query params (e.g. {@code sh_theme}) when it redirects
 * from {@code /protocol/openid-connect/auth} to {@code /login-actions/*}, so the
 * login theme JS never sees them. Setting the cookies here — before the 302 to
 * Keycloak — lets {@code theme-bridge.js} read {@code successhub_theme} on the
 * login page (same host {@code localhost}, any port).
 */
public class BridgePrefsCookieFilter extends OncePerRequestFilter {

    private static final Duration MAX_AGE = Duration.ofDays(365);

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path != null && path.startsWith("/oauth2/authorization")) {
            String theme = request.getParameter("sh_theme");
            if (theme != null) {
                String v = theme.trim().toLowerCase();
                if ("light".equals(v) || "dark".equals(v)) {
                    addCookie(response, "successhub_theme", v);
                }
            }
            String uiLocales = request.getParameter("ui_locales");
            if (uiLocales == null || uiLocales.isBlank()) {
                uiLocales = request.getParameter("kc_locale");
            }
            if (uiLocales != null && !uiLocales.isBlank()) {
                String v = uiLocales.trim().toLowerCase();
                String kc = v.startsWith("pl") ? "pl" : "en";
                addCookie(response, "successhub_locale", kc.equals("pl") ? "pl-PL" : "en-US");
                addCookie(response, "KEYCLOAK_LOCALE", kc);
            }
        }
        filterChain.doFilter(request, response);
    }

    private static void addCookie(HttpServletResponse response, String name, String value) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .httpOnly(false)
                .secure(false)
                .sameSite("Lax")
                .maxAge(MAX_AGE)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
