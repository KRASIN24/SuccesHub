package com.succeshub.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables binding of {@link KeycloakProperties} from {@code succeshub.keycloak.*}.
 */
@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
public class KeycloakClientConfig {
}
