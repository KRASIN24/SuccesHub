package com.successhub.coreinfra.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Sends HTML emails via Thymeleaf templates. Failures are logged and swallowed
 * so mail outages never block API request paths.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(MailProperties.class)
public class MailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final MailProperties mailProperties;

    /**
     * Renders {@code templateName} with {@code variables} and sends HTML mail.
     * Does nothing when {@code to} is blank. Never throws to the caller.
     *
     * @param to           recipient address
     * @param subject      subject line
     * @param templateName Thymeleaf template basename under {@code templates/}
     * @param variables    template model
     */
    public void sendTemplateMail(String to, String subject, String templateName, Map<String, Object> variables) {
        if (to == null || to.isBlank()) {
            log.debug("Skipping mail '{}': blank recipient", subject);
            return;
        }
        try {
            Context context = new Context();
            if (variables != null) {
                context.setVariables(variables);
            }
            String body = templateEngine.process(templateName, context);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            helper.setFrom(mailProperties.getFrom());
            helper.setTo(to.trim());
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(mimeMessage);
            log.info("Sent mail '{}' to {}", subject, to);
        } catch (MessagingException | RuntimeException ex) {
            log.warn("Failed to send mail '{}' to {}: {}", subject, to, ex.getMessage());
        }
    }
}
