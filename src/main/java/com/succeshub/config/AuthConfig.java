package com.succeshub.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers deploy-facing auth URL properties for the BFF security chain.
 */
@Configuration
@EnableConfigurationProperties(AuthProperties.class)
public class AuthConfig {
}
