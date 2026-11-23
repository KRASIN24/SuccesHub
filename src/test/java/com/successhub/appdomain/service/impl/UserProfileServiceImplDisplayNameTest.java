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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserProfileServiceImplDisplayNameTest {

    @Mock UserProfileRepository repository;
    @Mock MailService mailService;
    @Mock AuthProperties authProperties;

    @InjectMocks UserProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        when(authProperties.frontendOrigin()).thenReturn("http://localhost:4200");
    }

    @Test
    void updateDisplayName_marksProfileAsCustomized() {
        // Arrange
        UserProfile profile = new UserProfile();
        profile.setKeycloakId("kc-1");
        profile.setDisplayName("Old Name");

        when(repository.findByKeycloakId("kc-1")).thenReturn(Optional.of(profile));
        when(repository.save(any(UserProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var dto = service.updateDisplayName("kc-1", "  New Name  ");

        // Assert
        assertEquals("New Name", dto.displayName());
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(repository).save(captor.capture());
        assertEquals("New Name", captor.getValue().getDisplayName());
        assertTrue(captor.getValue().isDisplayNameCustomized());
    }

    @Test
    void getOrCreateProfile_doesNotOverwriteCustomizedName() {
        // Arrange
        UserProfile profile = new UserProfile();
        profile.setKeycloakId("kc-1");
        profile.setDisplayName("Custom Name");
        profile.setDisplayNameCustomized(true);

        when(repository.findByKeycloakId("kc-1")).thenReturn(Optional.of(profile));

        // Act
        var dto = service.getOrCreateProfile("kc-1", "Keycloak Name", "a@b.c");

        // Assert
        assertEquals("Custom Name", dto.displayName());
        verify(mailService, never()).sendTemplateMail(anyString(), anyString(), anyString(), anyMap());
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
                anyMap());
    }
}
