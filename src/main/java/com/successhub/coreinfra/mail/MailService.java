package com.successhub.coreinfra.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

/**
 * Enqueues HTML emails into the durable outbox. SMTP delivery is handled by
 * {@link MailOutboxSender} so request and event threads never wait on the mail server.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(MailProperties.class)
public class MailService {

    private final TemplateEngine templateEngine;
    private final MailOutboxRepository mailOutboxRepository;
    private final MailProperties mailProperties;

    /**
     * Renders {@code templateName} and inserts a {@link MailOutboxStatus#PENDING} outbox row.
     * Does nothing when {@code to} is blank. Never throws to the caller.
     */
    public void sendTemplateMail(String to, String subject, String templateName, Map<String, Object> variables) {
        sendTemplateMail(to, subject, templateName, variables, null, null);
    }

    /**
     * Enqueue with optional user id; locale defaults to {@code en-US}.
     */
    public void sendTemplateMail(
            String to,
            String subject,
            String templateName,
            Map<String, Object> variables,
            String userId) {
        sendTemplateMail(to, subject, templateName, variables, userId, null);
    }

    /**
     * Renders with the given locale (profile language) then enqueues.
     *
     * @param to           recipient address
     * @param subject      subject line (already localized by caller)
     * @param templateName Thymeleaf template basename under {@code templates/}
     * @param variables    template model
     * @param userId       Keycloak subject, or {@code null}
     * @param locale       profile locale for {@code #{...}} messages; {@code null} → en-US
     */
    @Transactional
    public void sendTemplateMail(
            String to,
            String subject,
            String templateName,
            Map<String, Object> variables,
            String userId,
            Locale locale) {
        if (to == null || to.isBlank()) {
            log.debug("Skipping mail '{}': blank recipient", subject);
            return;
        }
        try {
            Locale renderLocale = locale != null ? locale : MailLocales.fromProfile(null);
            Context context = new Context(renderLocale);
            if (variables != null) {
                context.setVariables(variables);
            }
            context.setVariable("htmlLang", renderLocale.getLanguage());
            String body = templateEngine.process(templateName, context);

            MailOutbox row = new MailOutbox();
            row.setUserId(userId != null && !userId.isBlank() ? userId.trim() : null);
            row.setToAddress(to.trim());
            row.setSubject(truncate(subject != null ? subject : templateName, 200));
            row.setTemplateName(truncate(templateName, 100));
            row.setBodyHtml(body);
            row.setStatus(MailOutboxStatus.PENDING);
            row.setAttemptCount(0);
            row.setMaxAttempts(Math.max(1, mailProperties.getOutboxMaxAttempts()));
            row.setNextAttemptAt(Instant.now());
            row.setCreatedAt(Instant.now());

            mailOutboxRepository.save(row);
            log.info("Enqueued mail '{}' to {} (outbox {})", row.getSubject(), row.getToAddress(), row.getId());
        } catch (RuntimeException ex) {
            log.warn("Failed to enqueue mail '{}' to {}: {}", subject, to, ex.getMessage());
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
