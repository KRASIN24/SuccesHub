package com.successhub.coreinfra.mail;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Persistence for the transactional mail outbox.
 */
public interface MailOutboxRepository extends JpaRepository<MailOutbox, UUID> {

    /**
     * Due {@link MailOutboxStatus#PENDING} rows ready for delivery or retry.
     *
     * @param now      current clock
     * @param pageable batch limit / order
     * @return due rows oldest-first
     */
    @Query("""
            SELECT m FROM MailOutbox m
            WHERE m.status = com.successhub.coreinfra.mail.MailOutboxStatus.PENDING
              AND m.nextAttemptAt <= :now
              AND m.attemptCount < m.maxAttempts
            ORDER BY m.nextAttemptAt ASC
            """)
    List<MailOutbox> findDue(@Param("now") Instant now, Pageable pageable);
}
