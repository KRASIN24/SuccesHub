package com.succeshub.coreinfra.auth;

import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.appdomain.repository.UserProfileRepository;
import com.succeshub.coreinfra.auth.dto.AccountStatusResponse;
import com.succeshub.coreinfra.auth.dto.MfaSetupResponse;
import com.succeshub.coreinfra.exception_handler.domain.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Default account self-service implementation (W8 + 30-day deactivation grace).
 */
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    public static final String DELETE_CONFIRMATION = "DELETE";
    public static final int DEACTIVATION_GRACE_DAYS = 30;

    private final KeycloakAdminClient adminClient;
    private final KeycloakAccountClient accountClient;
    private final UserProfileRepository userProfileRepository;

    @Override
    public AccountStatusResponse getStatus(OidcUser principal) {
        boolean mfa = adminClient.hasOtpCredential(principal.getSubject());
        UserProfile profile = findOrCreateProfile(principal.getSubject());
        Instant deactivatedAt = profile.getDeactivatedAt();
        boolean deactivated = deactivatedAt != null;
        Instant purgeAt = deactivated
                ? deactivatedAt.plus(DEACTIVATION_GRACE_DAYS, ChronoUnit.DAYS)
                : null;
        return new AccountStatusResponse(principal.getEmail(), mfa, deactivated, purgeAt);
    }

    @Override
    public void updateEmail(OidcUser principal, String email) {
        String trimmed = email.trim();
        adminClient.updateEmail(principal.getSubject(), trimmed);
    }

    @Override
    public void changePassword(
            OidcUser principal,
            OAuth2AuthorizedClient authorizedClient,
            String currentPassword,
            String newPassword) {
        if (currentPassword.equals(newPassword)) {
            throw new ValidationException("New password must be different from the current password");
        }
        accountClient.changePassword(authorizedClient, currentPassword, newPassword);
    }

    @Override
    public MfaSetupResponse mfaSetupRedirect() {
        return new MfaSetupResponse("/oauth2/authorization/keycloak?kc_action=CONFIGURE_TOTP");
    }

    @Override
    @Transactional
    public void deleteAccount(OidcUser principal, String confirmation) {
        if (!DELETE_CONFIRMATION.equals(confirmation)) {
            throw new ValidationException("Type DELETE to confirm account deletion");
        }
        UserProfile profile = findOrCreateProfile(principal.getSubject());
        profile.setDeactivatedAt(Instant.now());
        userProfileRepository.save(profile);
    }

    @Override
    @Transactional
    public void reactivate(OidcUser principal) {
        UserProfile profile = findOrCreateProfile(principal.getSubject());
        if (profile.getDeactivatedAt() == null) {
            return;
        }
        profile.setDeactivatedAt(null);
        userProfileRepository.save(profile);
    }

    private UserProfile findOrCreateProfile(String keycloakId) {
        return userProfileRepository.findByKeycloakId(keycloakId).orElseGet(() -> {
            UserProfile created = new UserProfile();
            created.setKeycloakId(keycloakId);
            return userProfileRepository.save(created);
        });
    }
}
