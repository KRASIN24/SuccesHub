package com.successhub.appdomain.service.impl;

import com.successhub.appdomain.dto.UpdatePreferencesRequest;
import com.successhub.appdomain.entity.UserProfile;
import com.successhub.appdomain.repository.UserProfileRepository;
import com.successhub.coreinfra.mail.MailService;
import com.successhub.coreinfra.security.AuthProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplPreferencesTest {

    @Mock UserProfileRepository repository;
    @Mock MailService mailService;
    @Mock AuthProperties authProperties;
    @Mock MessageSource messageSource;

    @InjectMocks UserProfileServiceImpl service;

    @Test
    void updatePreferences_setsDarkThemeAndLocale() {
        UserProfile profile = new UserProfile();
        profile.setKeycloakId("kc-1");
        profile.setDarkTheme(true);
        profile.setLocale("en-US");
        when(repository.findByKeycloakId("kc-1")).thenReturn(Optional.of(profile));
        when(repository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        var dto = service.updatePreferences("kc-1", new UpdatePreferencesRequest(
                false, "pl-PL", null, null));

        assertFalse(dto.darkTheme());
        assertEquals("pl-PL", dto.locale());
    }

    @Test
    void updatePreferences_rejectsUnsupportedLocale() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updatePreferences("kc-1", new UpdatePreferencesRequest(
                        null, "de-DE", null, null)));
    }

    @Test
    void updatePreferences_nullFieldsLeaveExistingValues() {
        UserProfile profile = new UserProfile();
        profile.setKeycloakId("kc-1");
        profile.setDarkTheme(false);
        profile.setLocale("pl-PL");
        profile.setNotifyInApp(false);
        profile.setEmailNotifications(false);
        when(repository.findByKeycloakId("kc-1")).thenReturn(Optional.of(profile));
        when(repository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        var dto = service.updatePreferences("kc-1", new UpdatePreferencesRequest(
                null, null, null, null));

        assertFalse(dto.darkTheme());
        assertEquals("pl-PL", dto.locale());
        assertFalse(dto.notifyInApp());
        assertFalse(dto.emailNotifications());
    }

    @Test
    void updatePreferences_setsNotificationFlags() {
        UserProfile profile = new UserProfile();
        profile.setKeycloakId("kc-1");
        when(repository.findByKeycloakId("kc-1")).thenReturn(Optional.of(profile));
        when(repository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        var dto = service.updatePreferences("kc-1", new UpdatePreferencesRequest(
                null, null, false, false));

        assertFalse(dto.notifyInApp());
        assertFalse(dto.emailNotifications());
        assertTrue(profile.isDarkTheme());
    }
}
