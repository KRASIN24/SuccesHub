package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.dto.AchievementDto;
import com.succeshub.appdomain.model.AchievementDefinition;
import com.succeshub.appdomain.model.UserAchievement;
import com.succeshub.appdomain.model.UserProfile;
import com.succeshub.appdomain.repository.AchievementDefinitionRepository;
import com.succeshub.appdomain.repository.GoalRepository;
import com.succeshub.appdomain.repository.UserAchievementRepository;
import com.succeshub.appdomain.service.AchievementEvaluator;
import com.succeshub.config.GamificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementEvaluatorImpl implements AchievementEvaluator {

    private final AchievementDefinitionRepository definitionRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final GoalRepository goalRepository;
    private final GamificationProperties properties;

    @Override
    @Transactional
    public List<AchievementDto> evaluateAndUnlock(String userId, UserProfile profile, long tasksCompletedToday) {
        Set<UUID> unlocked = userAchievementRepository.findByUserId(userId).stream()
                .map(ua -> ua.getAchievement().getId())
                .collect(Collectors.toSet());

        long completedGoals = goalRepository.countByUserIdAndStatus(userId, com.succeshub.appdomain.model.Goal.Status.COMPLETED);
        List<AchievementDto> newlyUnlocked = new ArrayList<>();

        for (AchievementDefinition def : definitionRepository.findAll()) {
            if (unlocked.contains(def.getId())) {
                continue;
            }
            if (!isEarned(def, profile, tasksCompletedToday, completedGoals)) {
                continue;
            }
            UserAchievement ua = new UserAchievement();
            ua.setUserId(userId);
            ua.setAchievement(def);
            ua.setUnlockedAt(Instant.now());
            userAchievementRepository.save(ua);
            newlyUnlocked.add(toDto(def, false, ua.getUnlockedAt()));
        }
        return newlyUnlocked;
    }

    private boolean isEarned(AchievementDefinition def, UserProfile profile, long tasksToday, long completedGoals) {
        return switch (def.getKey()) {
            case "SPEEDSTER" -> tasksToday >= properties.getSpeedsterTaskThreshold();
            case "PIONEER" -> completedGoals >= Math.max(1, def.getGoalsRequired());
            case "ARCHIVIST" -> profile.getLifetimeXp() >= def.getXpRequired();
            case "CONSISTENT" -> profile.getCurrentStreak() >= Math.max(def.getStreakRequired(), 7);
            case "SOVEREIGN" -> profile.getLevel() >= 50;
            default -> false;
        };
    }

    private AchievementDto toDto(AchievementDefinition def, boolean locked, Instant unlockedAt) {
        return new AchievementDto(def.getId(), def.getKey(), def.getLabel(), def.getIcon(), def.getDescription(), locked, unlockedAt);
    }
}
