package com.succeshub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Loot box feature flags and tunables exposed to operators via {@code application.yaml}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "succeshub.loot")
public class LootProperties {

    /**
     * When {@code false} (production default), manual grant/summon endpoints are disabled
     * and the frontend hides dev-only affordances.
     */
    private boolean enableDevGrants = false;
}
