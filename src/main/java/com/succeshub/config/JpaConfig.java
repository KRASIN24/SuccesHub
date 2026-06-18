package com.succeshub.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Registers JPA entity and repository scanning for the appdomain package.
 * Disabled under the {@code test} profile so lightweight {@code @SpringBootTest}
 * slices can mock repositories without an {@code EntityManagerFactory}.
 */
@Configuration
@Profile("!test")
@EnableJpaRepositories(basePackages = "com.succeshub.appdomain.repository")
@EntityScan(basePackages = "com.succeshub.appdomain.model")
public class JpaConfig {
}
