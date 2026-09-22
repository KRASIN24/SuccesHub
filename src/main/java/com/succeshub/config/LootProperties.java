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
     * When {@code false} (production default), manual loot grant/summon, streak
     * adjust/set/reset, and {@code /api/gamification/dev/*} endpoints return 403,
     * and the SPA hides the Dev bubble dock and page chips.
     * Shield day and inventory card use remain available.
     */
    private boolean enableDevGrants = false;
}
