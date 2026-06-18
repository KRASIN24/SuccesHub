package com.succeshub.appdomain.controller;

import com.succeshub.appdomain.dto.ProfileDto;
import com.succeshub.appdomain.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for the dashboard hero: user level, XP bar, streak, and global rank.
 * Auto-creates a profile row on first access for a new user.
 */
@Tag(name = "Profile", description = "User profile, XP, and leveling")
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserProfileService userProfileService;

    /**
     * Returns the authenticated user's profile, creating one if needed.
     */
    @Operation(summary = "Get user profile", description = "Returns level, current XP, next-level threshold, streak, and rank for the logged-in user.")
    @ApiResponse(responseCode = "200", description = "Profile returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @GetMapping
    public ResponseEntity<ProfileDto> getProfile(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        String displayName = principal.getPreferredUsername() != null
                ? principal.getPreferredUsername()
                : principal.getFullName();
        if (displayName == null) {
            displayName = "User";
        }
        ProfileDto dto = userProfileService.getOrCreateProfile(principal.getSubject(), displayName);
        return ResponseEntity.ok(dto);
    }
}
