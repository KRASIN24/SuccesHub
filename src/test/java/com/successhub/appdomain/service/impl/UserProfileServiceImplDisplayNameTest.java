package com.successhub.appdomain.service.impl;

import com.successhub.appdomain.entity.UserProfile;
import com.successhub.appdomain.repository.UserProfileRepository;
import com.successhub.coreinfra.mail.MailService;
import com.successhub.coreinfra.security.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.MessageSource;

import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserProfileServiceImplDisplayNameTest {

    @Mock UserProfileRepository repository;
    @Mock MailService mailService;
    @Mock AuthProperties authProperties;
    @Mock MessageSource messageSource;

    @InjectMocks UserProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        when(authProperties.frontendOrigin()).thenReturn("http://localhost:4200");
        when(messageSource.getMessage(anyString(), nullable(Object[].class), any(Locale.class)))
                .thenAnswer(inv -> {
                    String code = inv.getArgument(0);
                    if ("mail.welcome.subject".equals(code)) {
                        return "Welcome to SuccessHub";
                    }
                    Object[] args = inv.getArgument(1);
                    if (args != null && args.length > 0) {
                        return code + ":" + args[0];
                    }
                    return code;
                });
    }

    @Test
    void updateDisplayName_marksProfileAsCustomized() {
        UserProfile profile = new UserProfile();
        profile.setKeycloakId("kc-1");
        profile.setDisplayName("Old Name");

        when(repository.findByKeycloakId("kc-1")).thenReturn(Optional.of(profile));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = service.updateDisplayName("kc-1", "  New Name  ");

        assertEquals("New Name", dto.displayName());
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(repository).save(captor.capture());
        assertEquals("New Name", captor.getValue().getDisplayName());
        assertTrue(captor.getValue().isDisplayNameCustomized());
    }

    @Test
    void getOrCreateProfile_doesNotOverwriteCustomizedName() {
        UserProfile profile = new UserProfile();
        profile.setKeycloakId("kc-1");
        profile.setDisplayName("Custom Name");
        profile.setDisplayNameCustomized(true);

        when(repository.findByKeycloakId("kc-1")).thenReturn(Optional.of(profile));

        var dto = service.getOrCreateProfile("kc-1", "Keycloak Name", "a@b.c");

        assertEquals("Custom Name", dto.displayName());
        verify(mailService, never()).sendTemplateMail(
                anyString(), anyString(), anyString(), anyMap(), any(), any());
    }

    @Test
    void getOrCreateProfile_sendsWelcomeOnFirstCreate() {
        when(repository.findByKeycloakId("kc-new")).thenReturn(Optional.empty());
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.getOrCreateProfile("kc-new", "Ada", "ada@example.com");

        verify(mailService).sendTemplateMail(
                eq("ada@example.com"),
                eq("Welcome to SuccessHub"),
                eq("mail/welcome"),
                anyMap(),
                eq("kc-new"),
                any(Locale.class));
    }
}
