package com.succeshub.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({GamificationProperties.class, LootProperties.class})
public class GamificationConfig {
}
