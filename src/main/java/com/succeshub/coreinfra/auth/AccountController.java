package com.succeshub.coreinfra.auth;

import com.succeshub.coreinfra.auth.dto.AccountStatusResponse;
import com.succeshub.coreinfra.auth.dto.ChangePasswordRequest;
import com.succeshub.coreinfra.auth.dto.DeleteAccountRequest;
import com.succeshub.coreinfra.auth.dto.MfaSetupResponse;
import com.succeshub.coreinfra.auth.dto.UpdateEmailRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for in-app Keycloak account self-service (Settings → Account).
 * Tokens stay on the BFF; the SPA only uses the session cookie.
 */
@Tag(name = "Account", description = "Self-service email, password, MFA, and account deletion")
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * Returns email and MFA status for the Settings security section.
     */
    @Operation(summary = "Get account status", description = "Email from the session plus whether OTP is configured in Keycloak.")
    @ApiResponse(responseCode = "200", description = "Status returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @GetMapping
    public ResponseEntity<AccountStatusResponse> status(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(accountService.getStatus(principal));
    }

    /**
     * Updates the Keycloak account email via Admin API.
     */
    @Operation(summary = "Update email", description = "Sets a new email on the Keycloak user. Marks emailVerified false.")
    @ApiResponse(responseCode = "204", description = "Email updated")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @PostMapping("/email")
    public ResponseEntity<Void> updateEmail(
            @AuthenticationPrincipal OidcUser principal,
            @Valid @RequestBody UpdateEmailRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        accountService.updateEmail(principal, request.email());
        return ResponseEntity.noContent().build();
    }

    /**
     * Changes password via Keycloak Account API using the session access token.
     */
    @Operation(
            summary = "Change password",
            description = "Requires the current password. Uses Keycloak Account credentials API with the BFF-held access token."
    )
    @ApiResponse(responseCode = "204", description = "Password changed")
    @ApiResponse(responseCode = "400", description = "Validation failed or current password wrong")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal OidcUser principal,
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient,
            @Valid @RequestBody ChangePasswordRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        accountService.changePassword(
                principal,
                authorizedClient,
                request.currentPassword(),
                request.newPassword());
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns a same-origin URL that starts Keycloak Application-Initiated Action for TOTP.
     */
    @Operation(
            summary = "Start MFA setup",
            description = "Returns /oauth2/authorization/keycloak?kc_action=CONFIGURE_TOTP for a full-page redirect."
    )
    @ApiResponse(responseCode = "200", description = "Redirect URL returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @GetMapping("/mfa/setup")
    public ResponseEntity<MfaSetupResponse> mfaSetup(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(accountService.mfaSetupRedirect());
    }

    /**
     * Starts the 30-day deletion grace period, then clears the HTTP session.
     */
    @Operation(
            summary = "Deactivate account",
            description = "Requires confirmation string DELETE. Sets deactivated_at for a 30-day grace period, then ends the session. Data is purged after 30 days unless the user reactivates."
    )
    @ApiResponse(responseCode = "204", description = "Account deactivated")
    @ApiResponse(responseCode = "400", description = "Confirmation missing or invalid")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @DeleteMapping
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal OidcUser principal,
            @Valid @RequestBody DeleteAccountRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        accountService.deleteAccount(principal, request.confirmation());
        new SecurityContextLogoutHandler().logout(httpRequest, httpResponse, SecurityContextHolder.getContext().getAuthentication());
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    /**
     * Cancels a pending deletion during the grace period.
     */
    @Operation(
            summary = "Reactivate account",
            description = "Clears deactivated_at so the user can use the app again."
    )
    @ApiResponse(responseCode = "204", description = "Account reactivated")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @PostMapping("/reactivate")
    public ResponseEntity<Void> reactivate(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        accountService.reactivate(principal);
        return ResponseEntity.noContent().build();
    }
}
