package com.successhub.coreinfra.mail;

import java.util.Locale;

/**
 * Resolves BCP 47 profile locales to a {@link Locale} for mail rendering.
 */
public final class MailLocales {

    private MailLocales() {
    }

    /**
     * @param profileLocale profile {@code locale} column, e.g. {@code en-US} / {@code pl-PL}
     * @return locale for MessageSource / Thymeleaf; defaults to {@code en-US}
     */
    public static Locale fromProfile(String profileLocale) {
        if (profileLocale == null || profileLocale.isBlank()) {
            return Locale.forLanguageTag("en-US");
        }
        return Locale.forLanguageTag(profileLocale.trim());
    }
}
