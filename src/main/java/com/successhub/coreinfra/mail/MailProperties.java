package com.successhub.coreinfra.mail;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * From-address and related settings for transactional mail.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "successhub.mail")
public class MailProperties {

    /**
     * Envelope / From address used by {@link MailService}.
     */
    private String from = "noreply@successhub.local";
}
