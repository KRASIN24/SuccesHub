package com.succeshub.appdomain.service;

import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.appdomain.repository.UserProfileRepository;
import com.succeshub.coreinfra.auth.KeycloakAdminClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountPurgeSchedulerTest {

    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private UserDataCleanupService userDataCleanupService;
    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    private AccountPurgeScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new AccountPurgeScheduler(
                userProfileRepository, userDataCleanupService, keycloakAdminClient);
    }

    @Test
    void purgeExpiredDeactivations_purgesOnlyDueProfiles() {
        UserProfile due = new UserProfile();
        due.setKeycloakId("due-user");
        due.setDeactivatedAt(Instant.now().minus(31, ChronoUnit.DAYS));
        when(userProfileRepository.findByDeactivatedAtBefore(any(Instant.class)))
                .thenReturn(List.of(due));

        scheduler.purgeExpiredDeactivations();

        verify(userDataCleanupService).deleteAllForUser("due-user");
        verify(keycloakAdminClient).deleteUser("due-user");
    }

    @Test
    void purgeExpiredDeactivations_noopWhenNoneDue() {
        when(userProfileRepository.findByDeactivatedAtBefore(any(Instant.class)))
                .thenReturn(List.of());

        scheduler.purgeExpiredDeactivations();

        verify(userDataCleanupService, never()).deleteAllForUser(any());
        verify(keycloakAdminClient, never()).deleteUser(any());
    }
}
