package com.succeshub.appdomain.controller;

import com.succeshub.appdomain.dto.AchievementDto;
import com.succeshub.appdomain.service.AchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService service;

    @GetMapping
    public ResponseEntity<List<AchievementDto>> getAchievements(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.getAchievements(principal.getSubject()));
    }
}
