package com.succeshub.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.Assert;
import org.springframework.util.SerializationUtils;

import java.time.Duration;
import java.util.Base64;

/**
 * Cookie-backed {@link OAuth2AuthorizationRequest} store.
 *
 * <p>Keycloak self-registration can take long enough (or cross ports in such a way)
 * that the BFF HTTP session is gone when the browser returns to
 * {@code /login/oauth2/code/*}. Persisting the authorization request in a Lax
 * cookie avoids {@code authorization_request_not_found}.
 */
public final class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    public static final String COOKIE_NAME = "SUCCHUB_OAUTH2_AUTH_REQUEST";
    private static final Duration COOKIE_MAX_AGE = Duration.ofMinutes(30);

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Assert.notNull(request, "request cannot be null");
        return readCookie(request);
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        Assert.notNull(request, "request cannot be null");
        Assert.notNull(response, "response cannot be null");
        if (authorizationRequest == null) {
            deleteCookie(request, response);
            return;
        }
        writeCookie(response, serialize(authorizationRequest), COOKIE_MAX_AGE);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request,
            HttpServletResponse response) {
        Assert.notNull(response, "response cannot be null");
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        deleteCookie(request, response);
        return authorizationRequest;
    }

    private OAuth2AuthorizationRequest readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return deserialize(cookie.getValue());
            }
        }
        return null;
    }

    private void writeCookie(HttpServletResponse response, String value, Duration maxAge) {
        // Prefer ResponseCookie (SameSite=Lax) so the OAuth state survives the Keycloak hop.
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
                .path("/")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        // Fallback classic cookie for containers that drop unrecognized ResponseCookie attributes.
        Cookie legacy = new Cookie(COOKIE_NAME, value);
        legacy.setPath("/");
        legacy.setHttpOnly(true);
        legacy.setMaxAge((int) maxAge.getSeconds());
        response.addCookie(legacy);
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response) {
        if (readCookie(request) == null) {
            return;
        }
        writeCookie(response, "", Duration.ZERO);
    }

    private static String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        byte[] bytes = SerializationUtils.serialize(authorizationRequest);
        return Base64.getUrlEncoder().encodeToString(bytes);
    }

    private static OAuth2AuthorizationRequest deserialize(String value) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(value);
            Object object = SerializationUtils.deserialize(bytes);
            return object instanceof OAuth2AuthorizationRequest request ? request : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
