package com.succeshub.appdomain.controller;

import com.succeshub.appdomain.dto.ProfileDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.BossDamageDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.ClientConfigDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.CloseDayResultDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.DevCampaignDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.DevCampaignStateRequest;
import com.succeshub.appdomain.dto.gamification.GamificationDto.DevClockRequest;
import com.succeshub.appdomain.dto.gamification.GamificationDto.DevCombatStateRequest;
import com.succeshub.appdomain.dto.gamification.GamificationDto.DevDiagnosticsDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.DevInventoryGrantRequest;
import com.succeshub.appdomain.dto.gamification.GamificationDto.DevInventoryGrantResultDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.DevLevelRequest;
import com.succeshub.appdomain.dto.gamification.GamificationDto.DevRewardCatalogDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.DevSubscribeAllResultDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.DevXpRequest;
import com.succeshub.appdomain.service.DailyRitualService;
import com.succeshub.appdomain.service.DevGrantService;
import com.succeshub.appdomain.service.gamification.GamificationTimeUtil;
import com.succeshub.config.LootProperties;
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
import java.util.Map;
import java.util.UUID;

/**
 * Gated local/demo testing APIs: virtual day clock, ritual helpers, and dev grants.
 * Disabled unless {@code succeshub.loot.enable-dev-grants} is true.
 */
@Tag(name = "Dev tools", description = "Gated testing tools for clock, ritual, and dev grants")
@RestController
@RequestMapping("/api/gamification/dev")
@RequiredArgsConstructor
public class DevToolsController {

    private final LootProperties lootProperties;
    private final GamificationTimeUtil timeUtil;
    private final DailyRitualService dailyRitualService;
    private final DevGrantService devGrantService;

    /**
     * Sets or clears the process-wide virtual day offset used by gamification "today".
     */
    @Operation(summary = "Set virtual day offset",
            description = "Dev/testing tool: shifts gamification today by dayOffset days, or clears when clear=true. "
                    + "Requires succeshub.loot.enable-dev-grants.")
    @ApiResponse(responseCode = "200", description = "Clock updated")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Dev tools disabled")
    @PostMapping("/clock")
    public ResponseEntity<ClientConfigDto> setClock(
            @AuthenticationPrincipal OidcUser principal,
            @RequestBody DevClockRequest request) {
        ResponseEntity<ClientConfigDto> denied = denyIfDisabled(principal);
        if (denied != null) {
            return denied;
        }
        if (Boolean.TRUE.equals(request.clear())) {
            timeUtil.clearDayOffset();
        } else if (request.dayOffset() != null) {
            timeUtil.setDayOffset(request.dayOffset());
        }
        return ResponseEntity.ok(new ClientConfigDto(
                lootProperties.isEnableDevGrants(),
                timeUtil.today().toString(),
                timeUtil.getDayOffset()));
    }

    /**
     * Runs the same lazy day-close path as dashboard load (processes days through yesterday).
     */
    @Operation(summary = "Force close pending days",
            description = "Dev/testing tool: closes unprocessed days through yesterday without celebrating today. "
                    + "Requires succeshub.loot.enable-dev-grants.")
    @ApiResponse(responseCode = "200", description = "Close result returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Dev tools disabled")
    @PostMapping("/close-pending")
    public ResponseEntity<CloseDayResultDto> forceClosePending(@AuthenticationPrincipal OidcUser principal) {
        ResponseEntity<CloseDayResultDto> denied = denyIfDisabled(principal);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(dailyRitualService.closePendingDays(principal.getSubject()));
    }

    /**
     * Clears today's celebrate seal so the Seal CTA can show again.
     */
    @Operation(summary = "Unseal today",
            description = "Dev/testing tool: clears lastCelebratedDay when it equals effective today. "
                    + "Requires succeshub.loot.enable-dev-grants.")
    @ApiResponse(responseCode = "200", description = "Unseal result returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Dev tools disabled")
    @PostMapping("/unseal")
    public ResponseEntity<Map<String, Object>> unsealToday(@AuthenticationPrincipal OidcUser principal) {
        ResponseEntity<Map<String, Object>> denied = denyIfDisabled(principal);
        if (denied != null) {
            return denied;
        }
        boolean cleared = dailyRitualService.unsealToday(principal.getSubject());
        return ResponseEntity.ok(Map.of(
                "cleared", cleared,
                "effectiveToday", timeUtil.today().toString(),
                "message", cleared ? "Today unsealed." : "No seal for effective today."));
    }

    /**
     * Injects XP into the authenticated profile.
     */
    @Operation(summary = "Add XP",
            description = "Dev/testing tool: adds XP and applies level-ups. Requires succeshub.loot.enable-dev-grants.")
    @ApiResponse(responseCode = "200", description = "Profile updated")
    @ApiResponse(responseCode = "400", description = "Invalid amount")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Dev tools disabled")
    @PostMapping("/xp")
    public ResponseEntity<ProfileDto> addXp(
            @AuthenticationPrincipal OidcUser principal,
            @RequestBody DevXpRequest request) {
        ResponseEntity<ProfileDto> denied = denyIfDisabled(principal);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(devGrantService.addXp(principal.getSubject(), request.amount()));
    }

    /**
     * Sets absolute level on the authenticated profile.
     */
    @Operation(summary = "Set level",
            description = "Dev/testing tool: sets absolute level and clears XP into the level. "
                    + "Requires succeshub.loot.enable-dev-grants.")
    @ApiResponse(responseCode = "200", description = "Profile updated")
    @ApiResponse(responseCode = "400", description = "Invalid level")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Dev tools disabled")
    @PostMapping("/level")
    public ResponseEntity<ProfileDto> setLevel(
            @AuthenticationPrincipal OidcUser principal,
            @RequestBody DevLevelRequest request) {
        ResponseEntity<ProfileDto> denied = denyIfDisabled(principal);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(devGrantService.setLevel(principal.getSubject(), request.level()));
    }

    /**
     * Lists grantable reward catalog entries for the Dev inventory picker.
     */
    @Operation(summary = "List reward catalog",
            description = "Dev/testing tool: returns reward keys available to grant. "
                    + "Requires succeshub.loot.enable-dev-grants.")
    @ApiResponse(responseCode = "200", description = "Catalog returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Dev tools disabled")
    @GetMapping("/rewards")
    public ResponseEntity<List<DevRewardCatalogDto>> listRewards(@AuthenticationPrincipal OidcUser principal) {
        ResponseEntity<List<DevRewardCatalogDto>> denied = denyIfDisabled(principal);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(devGrantService.listRewards());
    }

    /**
     * Grants catalog rewards into inventory by key.
     */
    @Operation(summary = "Grant inventory item",
            description = "Dev/testing tool: stacks a catalog reward into inventory. "
                    + "Requires succeshub.loot.enable-dev-grants.")
    @ApiResponse(responseCode = "200", description = "Item granted")
    @ApiResponse(responseCode = "400", description = "Invalid quantity")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Dev tools disabled")
    @ApiResponse(responseCode = "404", description = "Reward key unknown")
    @PostMapping("/inventory")
    public ResponseEntity<DevInventoryGrantResultDto> grantInventory(
            @AuthenticationPrincipal OidcUser principal,
            @RequestBody DevInventoryGrantRequest request) {
        ResponseEntity<DevInventoryGrantResultDto> denied = denyIfDisabled(principal);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(devGrantService.grantInventory(
                principal.getSubject(), request.rewardKey(), request.quantity()));
    }

    /**
     * Sets banked shields and armed Double Strike / Surge flags.
     */
    @Operation(summary = "Set combat state",
            description = "Dev/testing tool: sets streakShields and/or armed card flags. "
                    + "Requires succeshub.loot.enable-dev-grants.")
    @ApiResponse(responseCode = "200", description = "Combat state updated")
    @ApiResponse(responseCode = "400", description = "Invalid shields value")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Dev tools disabled")
    @PostMapping("/combat-state")
    public ResponseEntity<ProfileDto> setCombatState(
            @AuthenticationPrincipal OidcUser principal,
            @RequestBody DevCombatStateRequest request) {
        ResponseEntity<ProfileDto> denied = denyIfDisabled(principal);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(devGrantService.setCombatState(principal.getSubject(), request));
    }

    /**
     * Lists active campaigns for Dev Campaigns tools.
     */
    @Operation(summary = "List active campaigns",
            description = "Dev/testing tool: active goals with pending Strike bank. "
                    + "Requires succeshub.loot.enable-dev-grants.")
    @ApiResponse(responseCode = "200", description = "Campaigns returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Dev tools disabled")
    @GetMapping("/campaigns")
    public ResponseEntity<List<DevCampaignDto>> listCampaigns(@AuthenticationPrincipal OidcUser principal) {
        ResponseEntity<List<DevCampaignDto>> denied = denyIfDisabled(principal);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(devGrantService.listActiveCampaigns(principal.getSubject()));
    }

    /**
     * Sets campaign progress and/or pending Strike damage.
     */
    @Operation(summary = "Set campaign state",
            description = "Dev/testing tool: set currentProgress and/or pendingDamage (clamped). "
                    + "Requires succeshub.loot.enable-dev-grants.")
    @ApiResponse(responseCode = "200", description = "Campaign updated")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Dev tools disabled")
    @ApiResponse(responseCode = "404", description = "Campaign not found")
    @PostMapping("/campaigns/{id}/state")
    public ResponseEntity<DevCampaignDto> setCampaignState(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Campaign (goal) ID") @PathVariable UUID id,
            @RequestBody DevCampaignStateRequest request) {
        ResponseEntity<DevCampaignDto> denied = denyIfDisabled(principal);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(devGrantService.setCampaignState(principal.getSubject(), id, request));
    }

    /**
     * Releases pending damage via the normal Strike path.
     */
    @Operation(summary = "Force Strike",
            description = "Dev/testing tool: calls Strike on the campaign. "
                    + "Requires succeshub.loot.enable-dev-grants.")
    @ApiResponse(responseCode = "200", description = "Strike applied")
    @ApiResponse(responseCode = "400", description = "No pending damage or inactive campaign")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Dev tools disabled")
    @ApiResponse(responseCode = "404", description = "Campaign not found")
    @PostMapping("/campaigns/{id}/strike")
    public ResponseEntity<BossDamageDto> strikeCampaign(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Campaign (goal) ID") @PathVariable UUID id) {
        ResponseEntity<BossDamageDto> denied = denyIfDisabled(principal);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(devGrantService.strike(principal.getSubject(), id));
    }

    /**
     * Subscribes every user category to the campaign (moves exclusive conflicts).
     */
    @Operation(summary = "Subscribe all categories",
            description = "Dev/testing tool: links all task categories to the campaign. "
                    + "Requires succeshub.loot.enable-dev-grants.")
    @ApiResponse(responseCode = "200", description = "Subscriptions updated")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Dev tools disabled")
    @ApiResponse(responseCode = "404", description = "Campaign not found")
    @PostMapping("/campaigns/{id}/subscribe")
    public ResponseEntity<DevSubscribeAllResultDto> subscribeAll(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Campaign (goal) ID") @PathVariable UUID id) {
        ResponseEntity<DevSubscribeAllResultDto> denied = denyIfDisabled(principal);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(devGrantService.subscribeAllCategories(principal.getSubject(), id));
    }

    /**
     * Returns a diagnostics snapshot for the Dev Bubble strip.
     */
    @Operation(summary = "Dev diagnostics",
            description = "Dev/testing tool: live profile / clock / campaign snapshot. "
                    + "Requires succeshub.loot.enable-dev-grants.")
    @ApiResponse(responseCode = "200", description = "Diagnostics returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "403", description = "Dev tools disabled")
    @GetMapping("/diagnostics")
    public ResponseEntity<DevDiagnosticsDto> diagnostics(@AuthenticationPrincipal OidcUser principal) {
        ResponseEntity<DevDiagnosticsDto> denied = denyIfDisabled(principal);
        if (denied != null) {
            return denied;
        }
        return ResponseEntity.ok(devGrantService.diagnostics(principal.getSubject()));
    }

    private <T> ResponseEntity<T> denyIfDisabled(OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        if (!lootProperties.isEnableDevGrants()) {
            return ResponseEntity.status(403).build();
        }
        return null;
    }
}
