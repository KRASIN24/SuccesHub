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
        XpCalculator.XpResult xp = xpCalculator.calculateForCompletion(task, profile, true);

        if (xp.totalXp() > 0) {
            profile.addXp(xp.totalXp());
            profile.setDailyXpEarned(profile.getDailyXpEarned() + xp.totalXp());
            profile.setFirstTaskCompletedToday(true);
        }

        XpEvent event = new XpEvent();
        event.setUserId(userId);
        event.setTaskId(task.getId());
        event.setBaseXp(xp.baseXp());
        event.setStreakBonus(xp.streakBonus());
        event.setFirstTaskBonus(xp.firstTaskBonus());
        event.setVariableBonus(xp.variableBonus());
        event.setChallengeBonus(xp.challengeBonus());
        event.setTotalXp(xp.totalXp());
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
                xp.baseXp(), xp.streakBonus(), xp.firstTaskBonus(), xp.variableBonus(), xp.challengeBonus(),
                xp.totalXp(), remaining);

        return new RewardEventDto(
                breakdown,
                profile.getLevel() > previousLevel,
                previousLevel,
                profile.getLevel(),
                xp.variableBonusTriggered(),
                bossDamage,
                unlocked,
                lootBoxId
        );
    }
}
