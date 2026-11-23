package com.successhub.coreinfra.mail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock TemplateEngine templateEngine;
    @Mock MailOutboxRepository mailOutboxRepository;

    MailProperties mailProperties;
    MailService mailService;

    @BeforeEach
    void setUp() {
        mailProperties = new MailProperties();
        mailProperties.setFrom("noreply@successhub.local");
        mailProperties.setOutboxMaxAttempts(5);
        mailService = new MailService(templateEngine, mailOutboxRepository, mailProperties);
    }

    @Test
    void sendTemplateMail_enqueuesPendingRow() {
        when(templateEngine.process(eq("mail/welcome"), any(Context.class))).thenReturn("<p>Hello</p>");
        when(mailOutboxRepository.save(any(MailOutbox.class))).thenAnswer(inv -> inv.getArgument(0));

        mailService.sendTemplateMail(
                "user@example.com", "Welcome", "mail/welcome", Map.of("displayName", "Ada"), "kc-1");

        ArgumentCaptor<MailOutbox> captor = ArgumentCaptor.forClass(MailOutbox.class);
        verify(mailOutboxRepository).save(captor.capture());
        MailOutbox row = captor.getValue();
        assertEquals("user@example.com", row.getToAddress());
        assertEquals("Welcome", row.getSubject());
        assertEquals("mail/welcome", row.getTemplateName());
        assertEquals("<p>Hello</p>", row.getBodyHtml());
        assertEquals(MailOutboxStatus.PENDING, row.getStatus());
        assertEquals("kc-1", row.getUserId());
        assertEquals(5, row.getMaxAttempts());
        assertEquals(0, row.getAttemptCount());
    }

    @Test
    void sendTemplateMail_skipsBlankRecipient() {
        mailService.sendTemplateMail("  ", "Welcome", "mail/welcome", Map.of());

        verify(templateEngine, never()).process(anyString(), any(Context.class));
        verify(mailOutboxRepository, never()).save(any());
    }

    @Test
    void sendTemplateMail_swallowsEnqueueFailures() {
        when(templateEngine.process(eq("mail/welcome"), any(Context.class))).thenReturn("<p>Hello</p>");
        when(mailOutboxRepository.save(any(MailOutbox.class))).thenThrow(new RuntimeException("db down"));

        assertDoesNotThrow(() ->
                mailService.sendTemplateMail("user@example.com", "Welcome", "mail/welcome", Map.of()));
    }

    @Test
    void sendTemplateMail_fourArgOverloadLeavesUserIdNull() {
        when(templateEngine.process(eq("mail/welcome"), any(Context.class))).thenReturn("<p>Hello</p>");
        when(mailOutboxRepository.save(any(MailOutbox.class))).thenAnswer(inv -> inv.getArgument(0));

        mailService.sendTemplateMail("user@example.com", "Welcome", "mail/welcome", Map.of());

        ArgumentCaptor<MailOutbox> captor = ArgumentCaptor.forClass(MailOutbox.class);
        verify(mailOutboxRepository).save(captor.capture());
        assertNull(captor.getValue().getUserId());
    }
}
