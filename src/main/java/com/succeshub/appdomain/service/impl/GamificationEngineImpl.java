package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.dto.AchievementDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.BossDamageDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.RewardEventDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.XpBreakdownDto;
import com.succeshub.appdomain.model.Task;
import com.succeshub.appdomain.model.UserLootBox;
import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.appdomain.model.XpEvent;
import com.succeshub.appdomain.repository.TaskRepository;
import com.succeshub.appdomain.repository.UserProfileRepository;
import com.succeshub.appdomain.repository.XpEventRepository;
import com.succeshub.appdomain.service.AchievementEvaluator;
import com.succeshub.appdomain.service.BossDamageService;
import com.succeshub.appdomain.service.GamificationEngine;
import com.succeshub.appdomain.service.LootBoxService;
import com.succeshub.appdomain.service.UserProfileService;
import com.succeshub.appdomain.service.gamification.GamificationTimeUtil;
import com.succeshub.appdomain.service.gamification.XpCalculator;
import com.succeshub.config.GamificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GamificationEngineImpl implements GamificationEngine {

    private static final int SURGE_TOKEN_XP = 50;

    private final UserProfileService userProfileService;
    private final UserProfileRepository profileRepository;
    private final XpEventRepository xpEventRepository;
    private final TaskRepository taskRepository;
    private final XpCalculator xpCalculator;
    private final GamificationProperties properties;
    private final GamificationTimeUtil timeUtil;
    private final BossDamageService bossDamageService;
    private final AchievementEvaluator achievementEvaluator;
    private final LootBoxService lootBoxService;

    @Override
    @Transactional
    public RewardEventDto processTaskCompletion(String userId, Task task) {
        if (xpEventRepository.existsByUserIdAndTaskId(userId, task.getId())) {
            return null;
        }

        UserProfile profile = userProfileService.requireProfile(userId);
        userProfileService.resetDailyCountersIfNeeded(profile, timeUtil.today());

        int previousLevel = profile.getLevel();
        XpCalculator.XpResult base = xpCalculator.calculateForCompletion(task, profile, true);

        boolean doubleStrike = profile.isPendingDoubleStrike();
        boolean surge = profile.isPendingSurgeToken();
        int rawTotal = base.baseXp() + base.streakBonus() + base.firstTaskBonus()
                + base.variableBonus() + base.challengeBonus();
        if (doubleStrike) {
            rawTotal *= 2;
        }
        int totalXp = xpCalculator.applyDailyCap(rawTotal, profile.getDailyXpEarned());
        if (surge) {
            totalXp += SURGE_TOKEN_XP;
        }
        if (doubleStrike) {
            profile.setPendingDoubleStrike(false);
        }
        if (surge) {
            profile.setPendingSurgeToken(false);
        }

        if (totalXp > 0) {
            profile.addXp(totalXp);
            profile.setDailyXpEarned(profile.getDailyXpEarned() + totalXp);
            profile.setFirstTaskCompletedToday(true);
        }

        XpEvent event = new XpEvent();
        event.setUserId(userId);
        event.setTaskId(task.getId());
        event.setBaseXp(base.baseXp());
        event.setStreakBonus(base.streakBonus());
        event.setFirstTaskBonus(base.firstTaskBonus());
        event.setVariableBonus(base.variableBonus());
        event.setChallengeBonus(base.challengeBonus());
        event.setTotalXp(totalXp);
        event.setDifficulty(task.getDifficulty());
        event.setWeeklyChallenge(task.isWeeklyChallenge());
        xpEventRepository.save(event);

        BossDamageDto bossDamage = bossDamageService.applyDamage(userId, task);
        if (bossDamage != null && bossDamage.goalCompleted() && bossDamage.goalXpReward() > 0) {
            profile.addXp(bossDamage.goalXpReward());
            profile.setDailyXpEarned(profile.getDailyXpEarned() + bossDamage.goalXpReward());
        }

        profileRepository.save(profile);

        long tasksToday = taskRepository.countCompletedInRange(
                userId, timeUtil.startOfDay(timeUtil.today()), timeUtil.endOfDay(timeUtil.today()));
        List<AchievementDto> unlocked = new ArrayList<>(achievementEvaluator.evaluateAndUnlock(userId, profile, tasksToday));

        UUID lootBoxId = null;
        for (AchievementDto ignored : unlocked) {
            lootBoxId = lootBoxService.grantLootBox(userId, UserLootBox.Source.ACHIEVEMENT);
        }

        int remaining = Math.max(0, properties.getDailyXpCap() - profile.getDailyXpEarned());
        XpBreakdownDto breakdown = new XpBreakdownDto(
                base.baseXp(), base.streakBonus(), base.firstTaskBonus(), base.variableBonus(), base.challengeBonus(),
                totalXp, remaining);

        return new RewardEventDto(
                breakdown,
                profile.getLevel() > previousLevel,
                previousLevel,
                profile.getLevel(),
                base.variableBonusTriggered(),
                bossDamage,
                unlocked,
                lootBoxId
        );
    }
}
