package com.successhub.appdomain.service.impl;

import com.successhub.appdomain.enums.TaskStatus;
import com.successhub.appdomain.enums.GoalStatus;
import com.successhub.appdomain.enums.LootBoxOpenStatus;
import com.successhub.appdomain.dto.ProfileDto;
import com.successhub.appdomain.dto.gamification.BossDamageDto;
import com.successhub.appdomain.dto.gamification.DevCampaignDto;
import com.successhub.appdomain.dto.gamification.DevCampaignStateRequest;
import com.successhub.appdomain.dto.gamification.DevCombatStateRequest;
import com.successhub.appdomain.dto.gamification.DevDiagnosticsDto;
import com.successhub.appdomain.dto.gamification.DevInventoryGrantResultDto;
import com.successhub.appdomain.dto.gamification.DevRewardCatalogDto;
import com.successhub.appdomain.dto.gamification.DevSubscribeAllResultDto;
import com.successhub.appdomain.dto.gamification.InventoryItemDto;
import com.successhub.appdomain.entity.Goal;
import com.successhub.appdomain.entity.GoalCategorySubscription;
import com.successhub.appdomain.entity.RewardDefinition;
import com.successhub.appdomain.entity.Task;
import com.successhub.appdomain.entity.TaskCategory;
import com.successhub.appdomain.entity.UserLootBox;
import com.successhub.appdomain.entity.UserProfile;
import com.successhub.appdomain.repository.GoalCategorySubscriptionRepository;
import com.successhub.appdomain.repository.GoalRepository;
import com.successhub.appdomain.repository.RewardDefinitionRepository;
import com.successhub.appdomain.repository.TaskCategoryRepository;
import com.successhub.appdomain.repository.TaskRepository;
import com.successhub.appdomain.repository.UserLootBoxRepository;
import com.successhub.appdomain.repository.UserProfileRepository;
import com.successhub.appdomain.service.BossDamageService;
import com.successhub.appdomain.service.DevGrantService;
import com.successhub.appdomain.service.LootBoxService;
import com.successhub.appdomain.service.UserProfileService;
import com.successhub.appdomain.service.gamification.GamificationTimeUtil;
import com.successhub.appdomain.config.GamificationProperties;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Implements gated Dev Bubble grants against existing domain services
 * (same {@code enable-dev-grants} gate as loot summon and streak tools).
 */
@Service
@RequiredArgsConstructor
public class DevGrantServiceImpl implements DevGrantService {

    private final UserProfileService profileService;
    private final UserProfileRepository profileRepository;
    private final LootBoxService lootBoxService;
    private final RewardDefinitionRepository rewardDefinitionRepository;
    private final GoalRepository goalRepository;
    private final GoalCategorySubscriptionRepository subscriptionRepository;
    private final TaskCategoryRepository categoryRepository;
    private final TaskRepository taskRepository;
    private final UserLootBoxRepository lootBoxRepository;
    private final BossDamageService bossDamageService;
    private final GamificationTimeUtil timeUtil;
    private final GamificationProperties gamificationProperties;

    @Override
    @Transactional
    public ProfileDto addXp(String userId, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        return profileService.addXp(userId, amount);
    }

    @Override
    @Transactional
    public ProfileDto setLevel(String userId, int level) {
        if (level < 1) {
            throw new IllegalArgumentException("level must be at least 1");
        }
        UserProfile profile = profileService.requireProfile(userId);
        profile.setLevel(level);
        profile.setCurrentXp(0);
        profile.setNextLevelXp(nextLevelXpFor(level));
        return profileService.mapToDto(profileRepository.save(profile));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DevRewardCatalogDto> listRewards() {
        return rewardDefinitionRepository.findAll().stream()
                .sorted(Comparator.comparing(RewardDefinition::getType)
                        .thenComparing(RewardDefinition::getRarity)
                        .thenComparing(RewardDefinition::getLabel))
                .map(r -> new DevRewardCatalogDto(
                        r.getKey(),
                        r.getLabel(),
                        r.getType().name(),
                        r.getRarity().name(),
                        r.getIcon()))
                .toList();
    }

    @Override
    @Transactional
    public DevInventoryGrantResultDto grantInventory(String userId, String rewardKey, int quantity) {
        InventoryItemDto item = lootBoxService.grantInventoryItem(userId, rewardKey, quantity);
        return new DevInventoryGrantResultDto(
                rewardKey,
                quantity,
                item.quantity(),
                "Granted " + quantity + "× " + item.reward().label() + " (stack " + item.quantity() + ").");
    }

    @Override
    @Transactional
    public ProfileDto setCombatState(String userId, DevCombatStateRequest request) {
        UserProfile profile = profileService.requireProfile(userId);
        if (request.streakShields() != null) {
            if (request.streakShields() < 0) {
                throw new IllegalArgumentException("streakShields must be non-negative");
            }
            profile.setStreakShields(request.streakShields());
        }
        if (request.pendingDoubleStrike() != null) {
            profile.setPendingDoubleStrike(request.pendingDoubleStrike());
        }
        if (request.pendingSurgeToken() != null) {
            profile.setPendingSurgeToken(request.pendingSurgeToken());
        }
        return profileService.mapToDto(profileRepository.save(profile));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DevCampaignDto> listActiveCampaigns(String userId) {
        return goalRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, GoalStatus.ACTIVE).stream()
                .map(g -> toCampaignDto(g, userId))
                .toList();
    }

    @Override
    @Transactional
    public DevCampaignDto setCampaignState(String userId, UUID goalId, DevCampaignStateRequest request) {
        Goal goal = requireActiveGoal(userId, goalId);
        if (request.currentProgress() != null) {
            double progress = Math.max(0.0, Math.min(goal.getTargetValue(), request.currentProgress()));
            goal.setCurrentProgress(progress);
        }
        if (request.pendingDamage() != null) {
            double maxPending = goal.getAbsoluteHealthRemaining();
            double pending = Math.max(0.0, Math.min(maxPending, request.pendingDamage()));
            goal.setPendingDamage(pending);
        }
        goalRepository.save(goal);
        return toCampaignDto(goal, userId);
    }

    @Override
    @Transactional
    public BossDamageDto strike(String userId, UUID goalId) {
        return bossDamageService.strike(userId, goalId);
    }

    @Override
    @Transactional
    public DevSubscribeAllResultDto subscribeAllCategories(String userId, UUID goalId) {
        Goal goal = requireActiveGoal(userId, goalId);
        int subscribed = 0;
        int alreadyLinked = 0;
        int moved = 0;

        for (TaskCategory category : categoryRepository.findByUserIdOrderBySortOrderAsc(userId)) {
            var existingOpt = subscriptionRepository.findByCategory_Id(category.getId());
            if (existingOpt.isPresent()) {
                GoalCategorySubscription existing = existingOpt.get();
                if (existing.getGoal().getId().equals(goalId)) {
                    alreadyLinked++;
                    continue;
                }
                subscriptionRepository.delete(existing);
                moved++;
            }
            GoalCategorySubscription sub = new GoalCategorySubscription();
            sub.setUserId(userId);
            sub.setGoal(goal);
            sub.setCategory(category);
            subscriptionRepository.save(sub);

            List<Task> openUnlinked = taskRepository.findByUserIdAndCategory_IdAndGoalIsNullAndStatusNot(
                    userId, category.getId(), TaskStatus.DONE);
            for (Task t : openUnlinked) {
                t.setGoal(goal);
            }
            if (!openUnlinked.isEmpty()) {
                taskRepository.saveAll(openUnlinked);
            }
            subscribed++;
        }

        return new DevSubscribeAllResultDto(
                subscribed,
                alreadyLinked,
                moved,
                "Subscribed " + subscribed + " categor"
                        + (subscribed == 1 ? "y" : "ies")
                        + (moved > 0 ? " (moved " + moved + ")" : "")
                        + (alreadyLinked > 0 ? "; " + alreadyLinked + " already linked" : "")
                        + ".");
    }

    @Override
    @Transactional(readOnly = true)
    public DevDiagnosticsDto diagnostics(String userId) {
        UserProfile profile = profileService.requireProfile(userId);
        var today = timeUtil.today();
        boolean sameDay = profile.getDailyXpDate() != null && profile.getDailyXpDate().equals(today);
        int dailyEarned = sameDay ? profile.getDailyXpEarned() : 0;
        int pendingLoot = lootBoxRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, LootBoxOpenStatus.PENDING)
                .size();
        double campaignPending = goalRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, GoalStatus.ACTIVE)
                .stream()
                .mapToDouble(Goal::getPendingDamage)
                .sum();
        int dailyRemaining = Math.max(0, gamificationProperties.getDailyXpCap() - dailyEarned);

        return new DevDiagnosticsDto(
                today.toString(),
                timeUtil.getDayOffset(),
                profile.getLastProcessedDay() != null ? profile.getLastProcessedDay().toString() : null,
                profile.getLastCelebratedDay() != null ? profile.getLastCelebratedDay().toString() : null,
                profile.getLastWeeklyLootWeek() != null ? profile.getLastWeeklyLootWeek().toString() : null,
                profile.getLevel(),
                profile.getCurrentXp(),
                profile.getNextLevelXp(),
                profile.getLifetimeXp(),
                dailyEarned,
                dailyRemaining,
                profile.getCurrentStreak(),
                profile.getStreakShields(),
                profile.isPendingDoubleStrike(),
                profile.isPendingSurgeToken(),
                pendingLoot,
                campaignPending);
    }

    private Goal requireActiveGoal(String userId, UUID goalId) {
        Goal goal = goalRepository.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found: " + goalId));
        if (goal.getStatus() != GoalStatus.ACTIVE) {
            throw new IllegalStateException("Campaign is not active");
        }
        return goal;
    }

    private DevCampaignDto toCampaignDto(Goal goal, String userId) {
        int subs = subscriptionRepository.findByGoal_IdAndUserId(goal.getId(), userId).size();
        return new DevCampaignDto(
                goal.getId(),
                goal.getName(),
                goal.getTargetValue(),
                goal.getCurrentProgress(),
                goal.getPendingDamage(),
                goal.getHealthRemaining(),
                subs);
    }

    /** Mirrors {@link UserProfile#addXp(int)} threshold growth from level 1. */
    static int nextLevelXpFor(int level) {
        int next = 1000;
        for (int i = 1; i < level; i++) {
            next = (int) (next * 1.2);
        }
        return next;
    }
}
