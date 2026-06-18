package com.succeshub.appdomain.controller;

import com.succeshub.appdomain.dto.GoalDto;
import com.succeshub.appdomain.service.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * REST API for Boss Battle goals: active bosses, conquered foes, and campaign summary.
 */
@Tag(name = "Goals", description = "Boss Battle goals and campaign progress")
@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService service;

    /**
     * Lists goals for the authenticated user, optionally filtered by status.
     */
    @Operation(summary = "List goals", description = "Returns active and completed goals, or only those matching the optional status filter.")
    @ApiResponse(responseCode = "200", description = "Goals returned")
    @ApiResponse(responseCode = "400", description = "Invalid status value")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @GetMapping
    public ResponseEntity<List<GoalDto.Response>> getGoals(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Filter by status: ACTIVE or COMPLETED")
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

    /**
     * Returns aggregate goal statistics for the grand campaign progress bar.
     */
    @Operation(summary = "Get goals summary", description = "Returns total goals, completed count, and overall completion percentage.")
    @ApiResponse(responseCode = "200", description = "Summary returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @GetMapping("/summary")
    public ResponseEntity<GoalDto.SummaryResponse> getSummary(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.getSummary(principal.getSubject()));
    }

    /**
     * Creates a new goal for the authenticated user.
     */
    @Operation(summary = "Create goal")
    @ApiResponse(responseCode = "200", description = "Goal created")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @PostMapping
    public ResponseEntity<GoalDto.Response> createGoal(
            @AuthenticationPrincipal OidcUser principal,
            @Valid @RequestBody GoalDto.CreateRequest req) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.create(principal.getSubject(), req));
    }

    /**
     * Updates an existing goal. Awards XP on first transition to COMPLETED.
     */
    @Operation(summary = "Update goal")
    @ApiResponse(responseCode = "200", description = "Goal updated")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Goal not found")
    @PutMapping("/{id}")
    public ResponseEntity<GoalDto.Response> updateGoal(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Goal ID") @PathVariable UUID id,
            @Valid @RequestBody GoalDto.UpdateRequest req) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.update(principal.getSubject(), id, req));
    }

    /**
     * Deletes a goal owned by the authenticated user.
     */
    @Operation(summary = "Delete goal")
    @ApiResponse(responseCode = "204", description = "Goal deleted")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Goal not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGoal(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Goal ID") @PathVariable UUID id) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        service.delete(principal.getSubject(), id);
        return ResponseEntity.noContent().build();
    }
}
