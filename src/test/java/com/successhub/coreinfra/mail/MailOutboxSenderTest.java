package com.successhub.coreinfra.mail;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailOutboxSenderTest {

    @Mock MailOutboxRepository mailOutboxRepository;
    @Mock JavaMailSender mailSender;

    MailProperties mailProperties;
    MailOutboxSender sender;

    @BeforeEach
    void setUp() {
        mailProperties = new MailProperties();
        mailProperties.setFrom("noreply@successhub.local");
        mailProperties.setOutboxBatchSize(10);
        mailProperties.setOutboxBackoffSeconds(5);
        sender = new MailOutboxSender(mailOutboxRepository, mailSender, mailProperties);
    }

    @Test
    void processDueMails_marksSentOnSuccess() throws Exception {
        MailOutbox row = pendingRow(0, 5);
        when(mailOutboxRepository.findDue(any(Instant.class), any(Pageable.class))).thenReturn(List.of(row));
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        sender.processDueMails();

        ArgumentCaptor<MailOutbox> captor = ArgumentCaptor.forClass(MailOutbox.class);
        verify(mailOutboxRepository).save(captor.capture());
        assertEquals(MailOutboxStatus.SENT, captor.getValue().getStatus());
        assertEquals(1, captor.getValue().getAttemptCount());
        assertNotNull(captor.getValue().getSentAt());
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void processDueMails_retriesWhenAttemptsRemain() {
        MailOutbox row = pendingRow(0, 5);
        when(mailOutboxRepository.findDue(any(Instant.class), any(Pageable.class))).thenReturn(List.of(row));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        org.mockito.Mockito.doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        Instant before = Instant.now();
        sender.processDueMails();

        ArgumentCaptor<MailOutbox> captor = ArgumentCaptor.forClass(MailOutbox.class);
        verify(mailOutboxRepository).save(captor.capture());
        MailOutbox saved = captor.getValue();
        assertEquals(MailOutboxStatus.PENDING, saved.getStatus());
        assertEquals(1, saved.getAttemptCount());
        assertEquals("smtp down", saved.getLastError());
        assertTrue(saved.getNextAttemptAt().isAfter(before));
    }

    @Test
    void processDueMails_marksFailedAtMaxAttempts() {
        MailOutbox row = pendingRow(4, 5);
        when(mailOutboxRepository.findDue(any(Instant.class), any(Pageable.class))).thenReturn(List.of(row));
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        org.mockito.Mockito.doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        sender.processDueMails();

        ArgumentCaptor<MailOutbox> captor = ArgumentCaptor.forClass(MailOutbox.class);
        verify(mailOutboxRepository).save(captor.capture());
        assertEquals(MailOutboxStatus.FAILED, captor.getValue().getStatus());
        assertEquals(5, captor.getValue().getAttemptCount());
    }

    private static MailOutbox pendingRow(int attemptCount, int maxAttempts) {
        MailOutbox row = new MailOutbox();
        row.setId(UUID.randomUUID());
        row.setToAddress("user@example.com");
        row.setSubject("Welcome");
        row.setTemplateName("mail/welcome");
        row.setBodyHtml("<p>Hi</p>");
        row.setStatus(MailOutboxStatus.PENDING);
        row.setAttemptCount(attemptCount);
        row.setMaxAttempts(maxAttempts);
        row.setNextAttemptAt(Instant.now().minusSeconds(1));
        row.setCreatedAt(Instant.now().minusSeconds(60));
        return row;
    }
}
