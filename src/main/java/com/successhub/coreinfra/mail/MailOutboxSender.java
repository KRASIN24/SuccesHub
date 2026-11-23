package com.successhub.coreinfra.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Polls the mail outbox and delivers due {@link MailOutboxStatus#PENDING} rows over SMTP
 * with exponential backoff retries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailOutboxSender {

    private final MailOutboxRepository mailOutboxRepository;
    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    /**
     * Claims a batch of due outbox rows and attempts SMTP delivery.
     */
    @Scheduled(fixedDelayString = "${successhub.mail.outbox-poll-ms:5000}")
    @Transactional
    public void processDueMails() {
        Instant now = Instant.now();
        int batch = Math.max(1, mailProperties.getOutboxBatchSize());
        List<MailOutbox> due = mailOutboxRepository.findDue(now, PageRequest.of(0, batch));
        for (MailOutbox row : due) {
            deliver(row, now);
        }
    }

    private void deliver(MailOutbox row, Instant now) {
        row.setAttemptCount(row.getAttemptCount() + 1);
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(row.getToAddress());
            helper.setSubject(row.getSubject());
            helper.setText(row.getBodyHtml(), true);

            mailSender.send(mimeMessage);

            row.setStatus(MailOutboxStatus.SENT);
            row.setSentAt(now);
            row.setLastError(null);
            mailOutboxRepository.save(row);
            log.info("Sent outbox mail '{}' to {} (id={})", row.getSubject(), row.getToAddress(), row.getId());
        } catch (MessagingException | RuntimeException ex) {
            String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            if (message.length() > 500) {
                message = message.substring(0, 500);
            }
            row.setLastError(message);

            if (row.getAttemptCount() >= row.getMaxAttempts()) {
                row.setStatus(MailOutboxStatus.FAILED);
                log.warn("Outbox mail {} failed permanently after {} attempts: {}",
                        row.getId(), row.getAttemptCount(), message);
            } else {
                row.setStatus(MailOutboxStatus.PENDING);
                row.setNextAttemptAt(now.plusSeconds(backoffSeconds(row.getAttemptCount())));
                log.warn("Outbox mail {} attempt {} failed; retry at {}: {}",
                        row.getId(), row.getAttemptCount(), row.getNextAttemptAt(), message);
            }
            mailOutboxRepository.save(row);
        }
    }

    private long backoffSeconds(int attemptCount) {
        int base = Math.max(1, mailProperties.getOutboxBackoffSeconds());
        long exp = (long) base * (1L << Math.min(attemptCount - 1, 4));
        return Math.min(60L, exp);
    }
}
