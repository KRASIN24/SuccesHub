package com.succeshub.appdomain.controller;

import com.succeshub.appdomain.dto.gamification.GamificationDto.AdjustStreakRequest;
import com.succeshub.appdomain.dto.gamification.GamificationDto.SetStreakRequest;
import com.succeshub.appdomain.dto.gamification.GamificationDto.ShieldDayRequest;
import com.succeshub.appdomain.dto.gamification.GamificationDto.StreakActionResultDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.StreakCalendarDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.UseItemResultDto;
import com.succeshub.appdomain.service.StreakManagementService;
import com.succeshub.config.LootProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/**
 * REST API for the profile streak page: month activity calendar, manual streak
 * adjustment, spending shields on missed days, and using gamification cards.
 */
@Tag(name = "Streak", description = "Streak calendar, manual adjustment, and card usage")
@RestController
@RequestMapping("/api/gamification")
@RequiredArgsConstructor
public class StreakController {

    private final StreakManagementService streakManagementService;
    private final LootProperties lootProperties;

    /**
     * Returns a month of streak activity with each day classified for calendar rendering.
     */
    @Operation(summary = "Streak calendar",
            description = "Per-day activity (completed/missed/shielded/today/future) for a month plus live streak figures.")
    @ApiResponse(responseCode = "200", description = "Calendar returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @GetMapping("/streak/calendar")
    public ResponseEntity<StreakCalendarDto> getCalendar(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Month to render as YYYY-MM; defaults to the current month")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        YearMonth target = month != null ? month : YearMonth.now();
        return ResponseEntity.ok(streakManagementService.getCalendar(principal.getSubject(), target));
    }

    /**
     * Changes the current streak by a signed delta (dev/testing tool).
     */
    @Operation(summary = "Adjust streak",
            description = "Dev/testing tool: adds or removes days from the current streak; clamped at zero.")
    @ApiResponse(responseCode = "200", description = "Streak updated")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Streak testing tools disabled")
    @PostMapping("/streak/adjust")
    public ResponseEntity<StreakActionResultDto> adjustStreak(
            @AuthenticationPrincipal OidcUser principal,
            @RequestBody AdjustStreakRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        if (!lootProperties.isEnableDevGrants()) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(streakManagementService.adjustStreak(principal.getSubject(), request.delta()));
    }

    /**
     * Sets the current streak to an absolute value (dev/testing tool).
     */
    @Operation(summary = "Set streak",
            description = "Dev/testing tool: sets the streak to a non-negative value and recomputes the tier.")
    @ApiResponse(responseCode = "200", description = "Streak updated")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Streak testing tools disabled")
    @PostMapping("/streak/set")
    public ResponseEntity<StreakActionResultDto> setStreak(
            @AuthenticationPrincipal OidcUser principal,
            @RequestBody SetStreakRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        if (!lootProperties.isEnableDevGrants()) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(streakManagementService.setStreak(principal.getSubject(), request.value()));
    }

    /**
     * Resets the current streak to zero (dev/testing tool).
     */
    @Operation(summary = "Reset streak", description = "Dev/testing tool: sets streak to zero and tier to NONE.")
    @ApiResponse(responseCode = "200", description = "Streak reset")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Streak testing tools disabled")
    @PostMapping("/streak/reset")
    public ResponseEntity<StreakActionResultDto> resetStreak(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        if (!lootProperties.isEnableDevGrants()) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(streakManagementService.resetStreak(principal.getSubject()));
    }

    /**
     * Spends a streak shield to protect (repair) a specific missed day.
     */
    @Operation(summary = "Shield a day",
            description = "Consumes one streak shield to protect a missed day; the day becomes shielded and the streak grows.")
    @ApiResponse(responseCode = "200", description = "Day shielded")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "422", description = "Day is future, already qualifying/shielded, or no shield available")
    @PostMapping("/streak/shield")
    public ResponseEntity<StreakActionResultDto> shieldDay(
            @AuthenticationPrincipal OidcUser principal,
            @RequestBody ShieldDayRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(streakManagementService.shieldDay(principal.getSubject(), request.date()));
    }

    /**
     * Consumes (uses) a utility inventory card, applying its effect.
     */
    @Operation(summary = "Use inventory card",
            description = "Banks a shield, arms Double Strike / Surge Token for the next task, "
                    + "or grants flat XP from a generic boost; cosmetics are equipped instead of used.")
    @ApiResponse(responseCode = "200", description = "Card used")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Inventory item not found")
    @ApiResponse(responseCode = "422", description = "Item is out of stock or not a consumable card")
    @PostMapping("/inventory/{id}/use")
    public ResponseEntity<UseItemResultDto> useCard(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Inventory item ID") @PathVariable UUID id) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(streakManagementService.useInventoryItem(principal.getSubject(), id));
    }
}
