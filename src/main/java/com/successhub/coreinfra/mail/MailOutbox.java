package com.successhub.coreinfra.mail;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Durable transactional-mail outbox row. Callers enqueue {@link MailOutboxStatus#PENDING};
 * {@link MailOutboxSender} delivers with retries.
 */
@Entity
@Table(name = "mail_outbox", schema = "successhub")
@Getter
@Setter
@NoArgsConstructor
public class MailOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "to_address", nullable = false, length = 320)
    private String toAddress;

    @Column(name = "subject", nullable = false, length = 200)
    private String subject;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Column(name = "body_html", nullable = false, columnDefinition = "TEXT")
    private String bodyHtml;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MailOutboxStatus status = MailOutboxStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 5;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "sent_at")
    private Instant sentAt;
}
