package com.succeshub.appdomain.controller;

import com.succeshub.appdomain.dto.gamification.GamificationDto.ClientConfigDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.CloseDayResultDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.DevClockRequest;
import com.succeshub.appdomain.service.DailyRitualService;
import com.succeshub.appdomain.service.gamification.GamificationTimeUtil;
import com.succeshub.config.LootProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Gated local/demo testing APIs: virtual day clock and ritual helpers.
 * Disabled unless {@code succeshub.loot.enable-dev-grants} is true.
 */
@Tag(name = "Dev tools", description = "Gated testing tools for clock override and ritual helpers")
@RestController
@RequestMapping("/api/gamification/dev")
@RequiredArgsConstructor
public class DevToolsController {

    private final LootProperties lootProperties;
    private final GamificationTimeUtil timeUtil;
    private final DailyRitualService dailyRitualService;

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
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        if (!lootProperties.isEnableDevGrants()) {
            return ResponseEntity.status(403).build();
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
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        if (!lootProperties.isEnableDevGrants()) {
            return ResponseEntity.status(403).build();
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
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        if (!lootProperties.isEnableDevGrants()) {
            return ResponseEntity.status(403).build();
        }
        boolean cleared = dailyRitualService.unsealToday(principal.getSubject());
        return ResponseEntity.ok(Map.of(
                "cleared", cleared,
                "effectiveToday", timeUtil.today().toString(),
                "message", cleared ? "Today unsealed." : "No seal for effective today."));
    }
}
