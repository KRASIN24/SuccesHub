package com.successhub.coreinfra.mail;

/**
 * Delivery lifecycle for a {@link MailOutbox} row.
 */
public enum MailOutboxStatus {
    PENDING,
    SENT,
    FAILED
}
