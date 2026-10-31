package com.succeshub.appdomain.controller;

import com.succeshub.appdomain.dto.GoalDto;
import com.succeshub.appdomain.dto.GoalSubscriptionDto;
import com.succeshub.appdomain.dto.TaskDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.BossDamageDto;
import com.succeshub.appdomain.service.BossDamageService;
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
 * REST API for Campaign goals: active campaigns, completed campaigns, links, and Strike.
 */
@Tag(name = "Goals", description = "Campaign goals, progress, category subscriptions, and Strike")
@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService service;
    private final BossDamageService bossDamageService;

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
     * Returns a single campaign owned by the authenticated user.
     */
    @Operation(summary = "Get goal by ID")
    @ApiResponse(responseCode = "200", description = "Goal returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Goal not found")
    @GetMapping("/{id}")
    public ResponseEntity<GoalDto.Response> getGoal(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Goal ID") @PathVariable UUID id) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.getById(principal.getSubject(), id));
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

    /**
     * Releases banked pending damage into the campaign HP bar (Strike / Final Strike).
     */
    @Operation(
            summary = "Strike campaign",
            description = "Applies pending damage from linked task completions to current progress, "
                    + "clears the pending bank, and completes the campaign when HP reaches zero.")
    @ApiResponse(responseCode = "200", description = "Strike applied")
    @ApiResponse(responseCode = "400", description = "No pending damage or campaign not active")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Goal not found")
    @PostMapping("/{id}/strike")
    public ResponseEntity<BossDamageDto> strikeGoal(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Goal ID") @PathVariable UUID id) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(bossDamageService.strike(principal.getSubject(), id));
    }

    /**
     * Lists tasks linked to this campaign.
     */
    @Operation(summary = "List linked tasks")
    @ApiResponse(responseCode = "200", description = "Tasks returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Goal not found")
    @GetMapping("/{id}/tasks")
    public ResponseEntity<List<TaskDto.Response>> getLinkedTasks(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Goal ID") @PathVariable UUID id) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.getLinkedTasks(principal.getSubject(), id));
    }

    /**
     * Attaches an existing task to this campaign.
     */
    @Operation(summary = "Attach task to campaign")
    @ApiResponse(responseCode = "200", description = "Task attached")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Goal or task not found")
    @PostMapping("/{id}/tasks/{taskId}")
    public ResponseEntity<TaskDto.Response> attachTask(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Goal ID") @PathVariable UUID id,
            @Parameter(description = "Task ID") @PathVariable UUID taskId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.attachTask(principal.getSubject(), id, taskId));
    }

    /**
     * Detaches a task from this campaign.
     */
    @Operation(summary = "Detach task from campaign")
    @ApiResponse(responseCode = "200", description = "Task detached")
    @ApiResponse(responseCode = "400", description = "Task not linked to this campaign")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Goal or task not found")
    @DeleteMapping("/{id}/tasks/{taskId}")
    public ResponseEntity<TaskDto.Response> detachTask(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Goal ID") @PathVariable UUID id,
            @Parameter(description = "Task ID") @PathVariable UUID taskId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.detachTask(principal.getSubject(), id, taskId));
    }

    /**
     * Lists category subscriptions for this campaign.
     */
    @Operation(summary = "List category subscriptions")
    @ApiResponse(responseCode = "200", description = "Subscriptions returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Goal not found")
    @GetMapping("/{id}/subscriptions")
    public ResponseEntity<List<GoalSubscriptionDto.Response>> listSubscriptions(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Goal ID") @PathVariable UUID id) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.listSubscriptions(principal.getSubject(), id));
    }

    /**
     * Subscribes a category to this campaign (exclusive per category).
     */
    @Operation(summary = "Subscribe category to campaign")
    @ApiResponse(responseCode = "200", description = "Subscribed")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Goal or category not found")
    @ApiResponse(responseCode = "409", description = "Category already subscribed elsewhere")
    @PostMapping("/{id}/subscriptions")
    public ResponseEntity<GoalSubscriptionDto.Response> subscribeCategory(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Goal ID") @PathVariable UUID id,
            @Valid @RequestBody GoalSubscriptionDto.SubscribeRequest req) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.subscribeCategory(principal.getSubject(), id, req));
    }

    /**
     * Unsubscribes a category from this campaign.
     */
    @Operation(summary = "Unsubscribe category from campaign")
    @ApiResponse(responseCode = "204", description = "Unsubscribed")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Subscription not found")
    @DeleteMapping("/{id}/subscriptions/{categoryId}")
    public ResponseEntity<Void> unsubscribeCategory(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Goal ID") @PathVariable UUID id,
            @Parameter(description = "Category ID") @PathVariable UUID categoryId) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        service.unsubscribeCategory(principal.getSubject(), id, categoryId);
        return ResponseEntity.noContent().build();
    }
}
