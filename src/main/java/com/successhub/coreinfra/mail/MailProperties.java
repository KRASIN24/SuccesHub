package com.successhub.coreinfra.mail;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * From-address and outbox worker settings for transactional mail.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "successhub.mail")
public class MailProperties {

    /**
     * Envelope / From address used when delivering outbox rows.
     */
    private String from = "noreply@successhub.local";

    /**
     * How often the outbox sender polls for due rows (milliseconds).
     */
    private long outboxPollMs = 5000L;

    /**
     * Max rows claimed per poll tick.
     */
    private int outboxBatchSize = 20;

    /**
     * Default max delivery attempts stored on new outbox rows.
     */
    private int outboxMaxAttempts = 5;

    /**
     * Base backoff seconds for the first retry; doubles each attempt, capped at 60s.
     */
    private int outboxBackoffSeconds = 5;
}
