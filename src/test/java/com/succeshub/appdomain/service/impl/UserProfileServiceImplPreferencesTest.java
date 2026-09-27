package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.appdomain.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplPreferencesTest {

    @Mock UserProfileRepository repository;

    @InjectMocks UserProfileServiceImpl service;

    @Test
    void updatePreferences_setsDarkThemeAndLocale() {
        UserProfile profile = new UserProfile();
        profile.setKeycloakId("kc-1");
        profile.setDarkTheme(true);
        profile.setLocale("en-US");
        when(repository.findByKeycloakId("kc-1")).thenReturn(Optional.of(profile));
        when(repository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        var dto = service.updatePreferences("kc-1", false, "pl-PL");

        assertFalse(dto.darkTheme());
        assertEquals("pl-PL", dto.locale());
    }

    @Test
    void updatePreferences_rejectsUnsupportedLocale() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updatePreferences("kc-1", null, "de-DE"));
    }

    @Test
    void updatePreferences_nullFieldsLeaveExistingValues() {
        UserProfile profile = new UserProfile();
        profile.setKeycloakId("kc-1");
        profile.setDarkTheme(false);
        profile.setLocale("pl-PL");
        when(repository.findByKeycloakId("kc-1")).thenReturn(Optional.of(profile));
        when(repository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        var dto = service.updatePreferences("kc-1", null, null);

        assertFalse(dto.darkTheme());
        assertEquals("pl-PL", dto.locale());
    }
}
