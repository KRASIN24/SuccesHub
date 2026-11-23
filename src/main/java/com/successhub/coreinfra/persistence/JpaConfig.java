package com.successhub.coreinfra.persistence;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Registers JPA entity and repository scanning for appdomain and coreinfra mail.
 * Disabled under the {@code test} profile so lightweight {@code @SpringBootTest}
 * slices can mock repositories without an {@code EntityManagerFactory}.
 */
@Configuration
@Profile("!test")
@EnableJpaRepositories(basePackages = {
        "com.successhub.appdomain.repository",
        "com.successhub.coreinfra.mail"
})
@EntityScan(basePackages = {
        "com.successhub.appdomain.entity",
        "com.successhub.coreinfra.mail"
})
public class JpaConfig {
}
