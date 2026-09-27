package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.appdomain.repository.UserProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplDisplayNameTest {

    @Mock UserProfileRepository repository;

    @InjectMocks UserProfileServiceImpl service;

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
        var dto = service.getOrCreateProfile("kc-1", "Keycloak Name");

        // Assert
        assertEquals("Custom Name", dto.displayName());
    }
}
