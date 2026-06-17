package com.succeshub.appdomain.controller;

import com.succeshub.appdomain.dto.ProfileDto;
import com.succeshub.appdomain.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserProfileService userProfileService;

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
