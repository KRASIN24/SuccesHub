package com.succeshub.coreinfra.auth;

import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.appdomain.repository.UserProfileRepository;
import com.succeshub.coreinfra.auth.dto.AccountStatusResponse;
import com.succeshub.coreinfra.exception_handler.domain.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private KeycloakAdminClient adminClient;
    @Mock
    private KeycloakAccountClient accountClient;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private OidcUser principal;
    @Mock
    private OAuth2AuthorizedClient authorizedClient;

    private AccountServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountServiceImpl(adminClient, accountClient, userProfileRepository);
    }

    @Test
    void getStatus_returnsEmailMfaAndActiveFlags() {
        when(principal.getEmail()).thenReturn("a@b.c");
        when(principal.getSubject()).thenReturn("sub-1");
        when(adminClient.hasOtpCredential("sub-1")).thenReturn(true);
        UserProfile profile = new UserProfile();
        profile.setKeycloakId("sub-1");
        when(userProfileRepository.findByKeycloakId("sub-1")).thenReturn(Optional.of(profile));

        AccountStatusResponse status = service.getStatus(principal);

        assertThat(status.email()).isEqualTo("a@b.c");
        assertThat(status.mfaEnabled()).isTrue();
        assertThat(status.deactivated()).isFalse();
        assertThat(status.purgeAt()).isNull();
    }

    @Test
    void getStatus_includesPurgeAtWhenDeactivated() {
        when(principal.getEmail()).thenReturn("a@b.c");
        when(principal.getSubject()).thenReturn("sub-1");
        when(adminClient.hasOtpCredential("sub-1")).thenReturn(false);
        Instant deactivatedAt = Instant.parse("2026-09-01T12:00:00Z");
        UserProfile profile = new UserProfile();
        profile.setKeycloakId("sub-1");
        profile.setDeactivatedAt(deactivatedAt);
        when(userProfileRepository.findByKeycloakId("sub-1")).thenReturn(Optional.of(profile));

        AccountStatusResponse status = service.getStatus(principal);

        assertThat(status.deactivated()).isTrue();
        assertThat(status.purgeAt()).isEqualTo(deactivatedAt.plus(30, ChronoUnit.DAYS));
    }

    @Test
    void changePassword_rejectsSamePassword() {
        assertThatThrownBy(() -> service.changePassword(principal, authorizedClient, "same", "same"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("different");
        verifyNoInteractions(accountClient);
    }

    @Test
    void deleteAccount_requiresConfirmation() {
        assertThatThrownBy(() -> service.deleteAccount(principal, "nope"))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(userProfileRepository, adminClient);
    }

    @Test
    void deleteAccount_setsDeactivatedAtWithoutPurge() {
        when(principal.getSubject()).thenReturn("sub-1");
        UserProfile profile = new UserProfile();
        profile.setKeycloakId("sub-1");
        when(userProfileRepository.findByKeycloakId("sub-1")).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deleteAccount(principal, "DELETE");

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getDeactivatedAt()).isNotNull();
        verify(adminClient, never()).deleteUser(any());
    }

    @Test
    void reactivate_clearsDeactivatedAt() {
        when(principal.getSubject()).thenReturn("sub-1");
        UserProfile profile = new UserProfile();
        profile.setKeycloakId("sub-1");
        profile.setDeactivatedAt(Instant.now());
        when(userProfileRepository.findByKeycloakId("sub-1")).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        service.reactivate(principal);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getDeactivatedAt()).isNull();
    }

    @Test
    void mfaSetupRedirect_usesKcAction() {
        assertThat(service.mfaSetupRedirect().redirectUrl())
                .isEqualTo("/oauth2/authorization/keycloak?kc_action=CONFIGURE_TOTP");
    }
}
