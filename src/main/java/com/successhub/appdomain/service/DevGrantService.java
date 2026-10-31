package com.successhub.appdomain.service;

import com.successhub.appdomain.dto.ProfileDto;
import com.successhub.appdomain.dto.gamification.BossDamageDto;
import com.successhub.appdomain.dto.gamification.DevCampaignDto;
import com.successhub.appdomain.dto.gamification.DevCampaignStateRequest;
import com.successhub.appdomain.dto.gamification.DevCombatStateRequest;
import com.successhub.appdomain.dto.gamification.DevDiagnosticsDto;
import com.successhub.appdomain.dto.gamification.DevInventoryGrantResultDto;
import com.successhub.appdomain.dto.gamification.DevRewardCatalogDto;
import com.successhub.appdomain.dto.gamification.DevSubscribeAllResultDto;

import java.util.List;
import java.util.UUID;

/**
 * Gated Dev Bubble grant helpers: progress, inventory, combat flags, and campaigns.
 * Callers must enforce {@code successhub.loot.enable-dev-grants} before invoking.
 */
public interface DevGrantService {

    /**
     * Adds XP to the profile (levels up as needed).
     *
     * @param userId Keycloak subject ID
     * @param amount XP to add; must be positive
     * @return updated profile snapshot
     */
    ProfileDto addXp(String userId, int amount);

    /**
     * Sets absolute level, clears XP into the level, and recomputes {@code nextLevelXp}.
     *
     * @param userId Keycloak subject ID
     * @param level  target level (minimum 1)
     * @return updated profile snapshot
     */
    ProfileDto setLevel(String userId, int level);

    /**
     * Lists grantable reward catalog entries for the inventory picker.
     *
     * @return ordered catalog rows
     */
    List<DevRewardCatalogDto> listRewards();

    /**
     * Grants catalog items into inventory by reward key.
     *
     * @param userId    Keycloak subject ID
     * @param rewardKey catalog key
     * @param quantity  stacks to add (positive)
     * @return grant result with total stack size
     */
    DevInventoryGrantResultDto grantInventory(String userId, String rewardKey, int quantity);

    /**
     * Updates banked shields and/or armed card flags.
     *
     * @param userId  Keycloak subject ID
     * @param request partial combat state (null fields unchanged)
     * @return updated profile snapshot
     */
    ProfileDto setCombatState(String userId, DevCombatStateRequest request);

    /**
     * Lists active campaigns with pending Strike bank and subscription counts.
     *
     * @param userId Keycloak subject ID
     * @return active campaigns newest first
     */
    List<DevCampaignDto> listActiveCampaigns(String userId);

    /**
     * Sets campaign progress and/or pending damage (clamped to remaining HP).
     *
     * @param userId  Keycloak subject ID
     * @param goalId  campaign ID
     * @param request partial state
     * @return updated campaign snapshot
     */
    DevCampaignDto setCampaignState(String userId, UUID goalId, DevCampaignStateRequest request);

    /**
     * Releases pending damage via the normal Strike path.
     *
     * @param userId Keycloak subject ID
     * @param goalId campaign ID
     * @return Strike result
     */
    BossDamageDto strike(String userId, UUID goalId);

    /**
     * Subscribes every user category to the campaign (moves exclusive conflicts).
     *
     * @param userId Keycloak subject ID
     * @param goalId campaign ID
     * @return counts of subscribed / already linked / moved
     */
    DevSubscribeAllResultDto subscribeAllCategories(String userId, UUID goalId);

    /**
     * Builds a diagnostics snapshot for the Dev Bubble strip.
     *
     * @param userId Keycloak subject ID
     * @return live diagnostics
     */
    DevDiagnosticsDto diagnostics(String userId);
}
