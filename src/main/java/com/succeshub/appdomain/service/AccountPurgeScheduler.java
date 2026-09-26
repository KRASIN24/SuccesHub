package com.succeshub.appdomain.service;

import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.appdomain.repository.UserProfileRepository;
import com.succeshub.coreinfra.auth.KeycloakAdminClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Permanently removes accounts whose 30-day deactivation grace period has ended.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AccountPurgeScheduler {

    private final UserProfileRepository userProfileRepository;
    private final UserDataCleanupService userDataCleanupService;
    private final KeycloakAdminClient keycloakAdminClient;

    /**
     * Daily at 04:15 — hard-delete Keycloak users and local data past the grace window.
     */
    @Scheduled(cron = "0 15 4 * * *")
    public void purgeExpiredDeactivations() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        List<UserProfile> due = userProfileRepository.findByDeactivatedAtBefore(cutoff);
        for (UserProfile profile : due) {
            purgeOne(profile.getKeycloakId());
        }
    }

    @Transactional
    void purgeOne(String keycloakUserId) {
        log.info("Purging deactivated account keycloakId={}", keycloakUserId);
        userDataCleanupService.deleteAllForUser(keycloakUserId);
        keycloakAdminClient.deleteUser(keycloakUserId);
    }
}
