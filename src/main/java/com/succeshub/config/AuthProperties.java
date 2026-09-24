package com.succeshub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Deploy-facing auth URLs for the BFF. Defaults match local Docker + Angular
 * ({@code localhost:4200} / Keycloak {@code :8080}); override via env or profile YAML.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "succeshub.auth")
public class AuthProperties {

    /**
     * Absolute SPA origin used for post-login and post-logout redirects.
     * Trailing slash is normalized for Spring's {@code defaultSuccessUrl}.
     */
    private String frontendUrl = "http://localhost:4200/";

    /**
     * Browser origins allowed by CORS (no trailing slash). Comma-separated in env
     * via {@code SUCCESSHUB_CORS_ORIGINS}.
     */
    private List<String> corsAllowedOrigins = new ArrayList<>(List.of("http://localhost:4200"));

    /**
     * Keycloak RP-initiated logout endpoint for the SuccessHub realm.
     */
    private String keycloakLogoutUri =
            "http://localhost:8080/realms/succeshub-realm/protocol/openid-connect/logout";

    /**
     * @return frontend URL with a trailing slash
     */
    public String frontendUrlWithSlash() {
        if (frontendUrl == null || frontendUrl.isBlank()) {
            return "http://localhost:4200/";
        }
        return frontendUrl.endsWith("/") ? frontendUrl : frontendUrl + "/";
    }

    /**
     * @return frontend origin without a trailing slash (for {@code post_logout_redirect_uri})
     */
    public String frontendOrigin() {
        String url = frontendUrlWithSlash();
        return url.substring(0, url.length() - 1);
    }
}
