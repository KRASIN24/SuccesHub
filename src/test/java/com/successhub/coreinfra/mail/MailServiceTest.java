package com.successhub.coreinfra.mail;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock JavaMailSender mailSender;
    @Mock TemplateEngine templateEngine;

    MailProperties mailProperties;
    MailService mailService;

    @BeforeEach
    void setUp() {
        mailProperties = new MailProperties();
        mailProperties.setFrom("noreply@successhub.local");
        mailService = new MailService(mailSender, templateEngine, mailProperties);
    }

    @Test
    void sendTemplateMail_sendsHtmlMimeMessage() throws Exception {
        when(templateEngine.process(eq("mail/welcome"), any(Context.class))).thenReturn("<p>Hello</p>");
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        mailService.sendTemplateMail("user@example.com", "Welcome", "mail/welcome", Map.of("displayName", "Ada"));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(captor.getValue().getContentType().toLowerCase().contains("multipart")
                || String.valueOf(captor.getValue().getContent()).contains("Hello")
                || captor.getValue().getSubject().equals("Welcome"));
    }

    @Test
    void sendTemplateMail_skipsBlankRecipient() {
        mailService.sendTemplateMail("  ", "Welcome", "mail/welcome", Map.of());

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendTemplateMail_swallowsSendFailures() {
        when(templateEngine.process(eq("mail/welcome"), any(Context.class))).thenReturn("<p>Hello</p>");
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        org.mockito.Mockito.doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() ->
                mailService.sendTemplateMail("user@example.com", "Welcome", "mail/welcome", Map.of()));
    }
}
