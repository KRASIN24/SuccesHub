package com.succeshub.appdomain.controller;

import com.succeshub.appdomain.dto.GoalDto;
import com.succeshub.appdomain.service.GoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService service;

    @GetMapping
    public ResponseEntity<List<GoalDto.Response>> getGoals(
            @AuthenticationPrincipal OidcUser principal,
            @RequestParam(required = false) String status) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        String userId = principal.getSubject();
        if (status != null && !status.isBlank()) {
            if ("COMPLETED".equalsIgnoreCase(status)) {
                return ResponseEntity.ok(service.getCompletedGoals(userId));
            } else if ("ACTIVE".equalsIgnoreCase(status)) {
                return ResponseEntity.ok(service.getActiveGoals(userId));
            } else {
                return ResponseEntity.badRequest().build();
            }
        }
        List<GoalDto.Response> all = new ArrayList<>();
        all.addAll(service.getActiveGoals(userId));
        all.addAll(service.getCompletedGoals(userId));
        return ResponseEntity.ok(all);
    }

    @GetMapping("/summary")
    public ResponseEntity<GoalDto.SummaryResponse> getSummary(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.getSummary(principal.getSubject()));
    }

    @PostMapping
    public ResponseEntity<GoalDto.Response> createGoal(
            @AuthenticationPrincipal OidcUser principal,
            @Valid @RequestBody GoalDto.CreateRequest req) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.create(principal.getSubject(), req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GoalDto.Response> updateGoal(
            @AuthenticationPrincipal OidcUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody GoalDto.UpdateRequest req) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.update(principal.getSubject(), id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(
            @AuthenticationPrincipal OidcUser principal,
            @PathVariable UUID id) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        service.delete(principal.getSubject(), id);
        return ResponseEntity.noContent().build();
    }
}
