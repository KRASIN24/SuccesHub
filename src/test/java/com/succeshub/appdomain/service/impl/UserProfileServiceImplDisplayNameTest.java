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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplDisplayNameTest {

    @Mock UserProfileRepository repository;

    @InjectMocks UserProfileServiceImpl service;

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

        var dto = service.getOrCreateProfile("kc-1", "Keycloak Name");

        assertEquals("Custom Name", dto.displayName());
    }

    @Test
    void requireProfile_reloadsProfileAfterProvisioning() {
        UserProfile persisted = new UserProfile();
        persisted.setKeycloakId("kc-new");

        when(repository.findByKeycloakId("kc-new"))
                .thenReturn(Optional.empty(), Optional.of(persisted));

        UserProfile result = service.requireProfile("kc-new");

        assertSame(persisted, result);
        verify(repository).insertIfAbsent(any(), eq("kc-new"), isNull());
        verify(repository, times(2)).findByKeycloakId("kc-new");
    }
}
