package com.succeshub.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Backend-For-Frontend (BFF) security setup.
 *
 * <p>The browser only ever holds an opaque {@code JSESSIONID} session cookie; the
 * OAuth2/OIDC tokens are obtained and kept server-side via {@code oauth2Login}.
 * CSRF is protected with a double-submit cookie that the Angular client reads
 * (XSRF-TOKEN) and echoes back (X-XSRF-TOKEN).
 *
 * <p>SPA origin, CORS, and Keycloak logout URL come from {@link AuthProperties}
 * so non-local deployments override them without code changes.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final AuthProperties authProperties;
    private final String oauthClientId;

    public SecurityConfig(
            AuthProperties authProperties,
            @Value("${spring.security.oauth2.client.registration.keycloak.client-id}") String oauthClientId) {
        this.authProperties = authProperties;
        this.oauthClientId = oauthClientId;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        String frontendUrl = authProperties.frontendUrlWithSlash();
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers("/public/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(endpoint -> endpoint
                                // Cookie store survives Keycloak registration (session can be lost
                                // while the browser is on :8080 for several minutes).
                                .authorizationRequestRepository(authorizationRequestRepository())
                                .authorizationRequestResolver(
                                        kcActionAuthorizationRequestResolver(clientRegistrationRepository))
                        )
                        .userInfoEndpoint(userInfo -> userInfo
                                .userAuthoritiesMapper(userAuthoritiesMapper())
                        )
                        // Always return to the SPA (absolute URL) so the redirect
                        // doesn't get expanded to the backend host behind the proxy.
                        .defaultSuccessUrl(frontendUrl, true)
                        .failureHandler(oauth2FailureHandler())
                )
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler())
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME)
                )
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**")
                        )
                );

        return http.build();
    }

    /**
     * Stores the OAuth2 authorization request in a browser cookie instead of the
     * HTTP session. Registration on Keycloak often outlives / replaces the BFF
     * session cookie when returning through the Angular proxy, which otherwise
     * surfaces as {@code authorization_request_not_found}.
     */
    @Bean
    public CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository() {
        return new CookieOAuth2AuthorizationRequestRepository();
    }

    /**
     * After a failed callback (e.g. lost state), send the browser back to the SPA
     * OAuth entry instead of Spring's default {@code /login?error} HTML page.
     */
    private AuthenticationFailureHandler oauth2FailureHandler() {
        return (request, response, exception) -> {
            // Do NOT bounce back into /oauth2/authorization — that loops into Keycloak's
            // "already logged in" page when an SSO session still exists.
            response.sendRedirect(authProperties.frontendUrlWithSlash() + "?auth_error=1");
        };
    }

    /**
     * Forwards {@code kc_action} query params into the Keycloak authorization
     * request so Settings can start Application-Initiated Actions (e.g. TOTP).
     */
    private OAuth2AuthorizationRequestResolver kcActionAuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver defaults =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository, "/oauth2/authorization");
        defaults.setAuthorizationRequestCustomizer(customizer -> {
            // no-op default; real customization happens in the wrapping resolver below
        });

        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                return customize(defaults.resolve(request), request);
            }

            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
                return customize(defaults.resolve(request, clientRegistrationId), request);
            }

            private OAuth2AuthorizationRequest customize(
                    OAuth2AuthorizationRequest authorizationRequest,
                    HttpServletRequest request) {
                if (authorizationRequest == null) {
                    return null;
                }
                OAuth2AuthorizationRequest.Builder builder =
                        OAuth2AuthorizationRequest.from(authorizationRequest);

                // Self-registration entry: use Keycloak's registrations endpoint so
                // Create account cannot run under an existing SSO browser session
                // without first going through logout → ?register=1 → this path.
                String register = request.getParameter("register");
                if ("1".equals(register) || "true".equalsIgnoreCase(register)) {
                    builder.authorizationUri(authProperties.keycloakRegistrationsUri());
                }

                String kcAction = request.getParameter("kc_action");
                if (kcAction != null && !kcAction.isBlank()) {
                    Map<String, Object> extra = new HashMap<>(authorizationRequest.getAdditionalParameters());
                    extra.put("kc_action", kcAction);
                    builder.additionalParameters(extra);
                }
                return builder.build();
            }
        };
    }

    /**
     * Sends an RP-initiated logout to Keycloak so the Keycloak SSO session is
     * also terminated, then returns the browser to the SPA.
     *
     * <p>Built manually (rather than via {@code OidcClientInitiatedLogoutSuccessHandler})
     * because the provider is configured with explicit endpoints instead of
     * {@code issuer-uri}, so the discovered {@code end_session_endpoint} is not
     * available in the client registration metadata.
     */
    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        return (request, response, authentication) -> {
            // Always end the Keycloak SSO session. Skipping this (e.g. when the
            // principal is already cleared) leaves an active KC cookie so the SPA
            // authGuard immediately silent-logs the user back in — "logout does nothing".
            String frontendUrl = authProperties.frontendUrlWithSlash();
            UriComponentsBuilder logout = UriComponentsBuilder
                    .fromUriString(authProperties.getKeycloakLogoutUri())
                    .queryParam("client_id", oauthClientId)
                    .queryParam("post_logout_redirect_uri", frontendUrl);

            if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser
                    && oidcUser.getIdToken() != null) {
                logout.queryParam("id_token_hint", oidcUser.getIdToken().getTokenValue());
            }

            response.sendRedirect(logout.build().toUriString());
        };
    }

    /**
     * Maps Keycloak realm roles (claim {@code realm_access.roles}) to Spring
     * Security {@code ROLE_*} authorities, mirroring the previous resource-server
     * behaviour for the session-based principal.
     */
    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return authorities -> {
            Set<GrantedAuthority> mapped = new HashSet<>();
            authorities.forEach(authority -> {
                mapped.add(authority);
                if (authority instanceof OidcUserAuthority oidcAuthority) {
                    Map<String, Object> realmAccess = oidcAuthority.getIdToken().getClaimAsMap("realm_access");
                    if (realmAccess == null && oidcAuthority.getUserInfo() != null) {
                        realmAccess = oidcAuthority.getUserInfo().getClaimAsMap("realm_access");
                    }
                    if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> roles) {
                        roles.forEach(role -> mapped.add(new SimpleGrantedAuthority("ROLE_" + role)));
                    }
                }
            });
            return mapped;
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = authProperties.getCorsAllowedOrigins();
        config.setAllowedOrigins(origins == null || origins.isEmpty()
                ? List.of("http://localhost:4200", "http://localhost:8080")
                : List.copyOf(origins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
