package com.succeshub.appdomain.controller;

import com.succeshub.appdomain.dto.gamification.GamificationDto.BoxTypeDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.CloseDayResultDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.DailyStatusDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.ForecastDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.GrantBoxRequest;
import com.succeshub.appdomain.dto.gamification.GamificationDto.InventoryItemDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.LootBoxDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.WeeklyInsightDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.XpPreviewDto;
import com.succeshub.appdomain.model.LootBoxType;
import com.succeshub.appdomain.service.DailyRitualService;
import com.succeshub.appdomain.service.GamificationService;
import com.succeshub.appdomain.service.InsightService;
import com.succeshub.appdomain.service.LootBoxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for gamification: XP preview, daily ritual status, forecasts, and weekly insights.
 */
@Tag(name = "Gamification", description = "XP engine, daily ritual, streaks, and insights")
@RestController
@RequestMapping("/api/gamification")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;
    private final DailyRitualService dailyRitualService;
    private final InsightService insightService;
    private final LootBoxService lootBoxService;

    /**
     * Estimates XP for a task configuration without persisting.
     */
    @Operation(summary = "Preview XP", description = "Returns XP breakdown for difficulty, duration, and priority.")
    @ApiResponse(responseCode = "200", description = "Preview returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @GetMapping("/xp-preview")
    public ResponseEntity<XpPreviewDto> previewXp(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Task difficulty 1–5") @RequestParam(defaultValue = "3") int difficulty,
            @Parameter(description = "Duration in minutes") @RequestParam(defaultValue = "30") int durationMinutes,
            @Parameter(description = "Priority 1–3") @RequestParam(defaultValue = "2") int priority,
            @Parameter(description = "Weekly challenge flag") @RequestParam(defaultValue = "false") boolean weeklyChallenge) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(gamificationService.previewXp(
                principal.getSubject(), difficulty, durationMinutes, priority, weeklyChallenge));
    }

    /**
     * Returns daily ritual status; runs lazy day-close first.
     */
    @Operation(summary = "Daily status", description = "Lazy-closes pending days then returns today's mission summary.")
    @ApiResponse(responseCode = "200", description = "Status returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @GetMapping("/daily")
    public ResponseEntity<DailyStatusDto> getDaily(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(dailyRitualService.getDailyStatus(principal.getSubject()));
    }

    /**
     * Optional celebration endpoint; streak integrity does not depend on this call.
     */
    @Operation(summary = "Close day", description = "Optional celebration trigger; same logic as lazy close on GET /daily.")
    @ApiResponse(responseCode = "200", description = "Close result returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @PostMapping("/close-day")
    public ResponseEntity<CloseDayResultDto> closeDay(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(dailyRitualService.closePendingDays(principal.getSubject()));
    }

    /**
     * Returns tomorrow forecast and weekly challenge teaser data.
     */
    @Operation(summary = "Forecast", description = "Next streak milestone and weekly challenge rules.")
    @ApiResponse(responseCode = "200", description = "Forecast returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @GetMapping("/forecast")
    public ResponseEntity<ForecastDto> getForecast(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(dailyRitualService.getForecast(principal.getSubject()));
    }

    /**
     * Returns week-over-week competence metrics.
     */
    @Operation(summary = "Weekly insights", description = "Comparative task and XP metrics vs prior week.")
    @ApiResponse(responseCode = "200", description = "Insights returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @GetMapping("/insights/weekly")
    public ResponseEntity<WeeklyInsightDto> getWeeklyInsights(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(insightService.getWeeklyInsights(principal.getSubject()));
    }

    /**
     * Lists the catalog of selectable box types and their rarity odds.
     */
    @Operation(summary = "List box types", description = "Returns the three box types with their rarity weights.")
    @ApiResponse(responseCode = "200", description = "Box types returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @GetMapping("/loot-boxes/types")
    public ResponseEntity<List<BoxTypeDto>> getBoxTypes(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(lootBoxService.getBoxTypes());
    }

    /**
     * Lists pending loot boxes for the user.
     */
    @Operation(summary = "List loot boxes", description = "Returns the user's unopened loot boxes.")
    @ApiResponse(responseCode = "200", description = "Loot boxes returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @GetMapping("/loot-boxes")
    public ResponseEntity<List<LootBoxDto>> getLootBoxes(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(lootBoxService.getPendingLootBoxes(principal.getSubject()));
    }

    /**
     * Grants (summons) a pending loot box of the requested type to the user.
     */
    @Operation(summary = "Grant loot box", description = "Creates a pending box of the requested type for the user.")
    @ApiResponse(responseCode = "200", description = "Loot box granted")
    @ApiResponse(responseCode = "400", description = "Unknown box type")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @PostMapping("/loot-boxes/grant")
    public ResponseEntity<LootBoxDto> grantLootBox(
            @AuthenticationPrincipal OidcUser principal,
            @RequestBody GrantBoxRequest request) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        LootBoxType type;
        try {
            type = LootBoxType.valueOf(request.boxType());
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(lootBoxService.grantBox(principal.getSubject(), type));
    }

    /**
     * Opens a loot box and rolls rewards into inventory.
     */
    @Operation(summary = "Open loot box", description = "Reveals card-flip rewards and adds them to inventory.")
    @ApiResponse(responseCode = "200", description = "Loot box opened")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Loot box not found")
    @PostMapping("/loot-boxes/{id}/open")
    public ResponseEntity<LootBoxDto> openLootBox(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Loot box ID") @PathVariable UUID id) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(lootBoxService.openLootBox(principal.getSubject(), id));
    }

    /**
     * Returns the user's inventory from loot rewards.
     */
    @Operation(summary = "Get inventory", description = "Lists shields, boosts, and cosmetic items.")
    @ApiResponse(responseCode = "200", description = "Inventory returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @GetMapping("/inventory")
    public ResponseEntity<List<InventoryItemDto>> getInventory(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(lootBoxService.getInventory(principal.getSubject()));
    }

    /**
     * Toggles the equipped state of a cosmetic inventory item (title or frame).
     */
    @Operation(summary = "Equip inventory item",
            description = "Toggles equip on a title or frame; equipping one unequips others of the same type.")
    @ApiResponse(responseCode = "200", description = "Inventory item updated")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Inventory item not found")
    @PostMapping("/inventory/{id}/equip")
    public ResponseEntity<InventoryItemDto> toggleEquip(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Inventory item ID") @PathVariable UUID id) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(lootBoxService.toggleEquip(principal.getSubject(), id));
    }
}
