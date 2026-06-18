package com.succeshub.appdomain.controller;

import com.succeshub.appdomain.dto.AchievementDto;
import com.succeshub.appdomain.service.AchievementService;
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

import java.util.List;

/**
 * REST API for dashboard merits: achievement definitions with locked/unlocked state.
 */
@Tag(name = "Achievements", description = "Merit badges and unlock status")
@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService service;

    /**
     * Returns all achievement definitions annotated with the user's unlock state.
     */
    @Operation(summary = "List achievements", description = "Returns the full merit catalog with locked flag and optional unlockedAt timestamp.")
    @ApiResponse(responseCode = "200", description = "Achievements returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @GetMapping
    public ResponseEntity<List<AchievementDto>> getAchievements(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.getAchievements(principal.getSubject()));
    }
}
