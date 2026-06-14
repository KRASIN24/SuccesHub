package com.succeshub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Backend-For-Frontend (BFF) security setup.
 *
 * The browser only ever holds an opaque {@code JSESSIONID} session cookie; the
 * OAuth2/OIDC tokens are obtained and kept server-side via {@code oauth2Login}.
 * CSRF is protected with a double-submit cookie that the Angular client reads
 * (XSRF-TOKEN) and echoes back (X-XSRF-TOKEN).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final String FRONTEND_URL = "http://localhost:4200/";
    private static final String KEYCLOAK_LOGOUT_URI =
            "http://localhost:8080/realms/succeshub-realm/protocol/openid-connect/logout";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
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
                        .userInfoEndpoint(userInfo -> userInfo
                                .userAuthoritiesMapper(userAuthoritiesMapper())
                        )
                        // Always return to the SPA (absolute URL) so the redirect
                        // doesn't get expanded to the backend host behind the proxy.
                        .defaultSuccessUrl(FRONTEND_URL, true)
                )
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler())
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
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
     * Sends an RP-initiated logout to Keycloak so the Keycloak SSO session is
     * also terminated, then returns the browser to the SPA.
     *
     * Built manually (rather than via {@code OidcClientInitiatedLogoutSuccessHandler})
     * because the provider is configured with explicit endpoints instead of
     * {@code issuer-uri}, so the discovered {@code end_session_endpoint} is not
     * available in the client registration metadata.
     */
    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        return (request, response, authentication) -> {
            String targetUrl = FRONTEND_URL;
            if (authentication != null && authentication.getPrincipal() instanceof OidcUser oidcUser) {
                targetUrl = UriComponentsBuilder
                        .fromUriString(KEYCLOAK_LOGOUT_URI)
                        .queryParam("id_token_hint", oidcUser.getIdToken().getTokenValue())
                        .queryParam("post_logout_redirect_uri", FRONTEND_URL)
                        .build()
                        .toUriString();
            }
            response.sendRedirect(targetUrl);
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
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
