package com.successhub.appdomain.service.impl;

import com.successhub.appdomain.enums.GoalStatus;
import com.successhub.appdomain.dto.AchievementDto;
import com.successhub.appdomain.entity.AchievementDefinition;
import com.successhub.appdomain.entity.UserAchievement;
import com.successhub.appdomain.entity.UserProfile;
import com.successhub.appdomain.repository.AchievementDefinitionRepository;
import com.successhub.appdomain.repository.GoalRepository;
import com.successhub.appdomain.event.GameNotificationEvent;
import com.successhub.appdomain.enums.NotificationType;
import com.successhub.appdomain.repository.UserAchievementRepository;
import com.successhub.appdomain.service.AchievementEvaluator;
import com.successhub.appdomain.config.GamificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public List<AchievementDto> evaluateAndUnlock(String userId, UserProfile profile, long tasksCompletedToday) {
        Set<UUID> unlocked = userAchievementRepository.findByUserId(userId).stream()
                .map(ua -> ua.getAchievement().getId())
                .collect(Collectors.toSet());

        long completedGoals = goalRepository.countByUserIdAndStatus(userId, com.successhub.appdomain.enums.GoalStatus.COMPLETED);
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
            AchievementDto dto = toDto(def, false, ua.getUnlockedAt());
            newlyUnlocked.add(dto);
            eventPublisher.publishEvent(new GameNotificationEvent(
                    userId,
                    NotificationType.ACHIEVEMENT_UNLOCKED,
                    "Achievement unlocked",
                    dto.label(),
                    "/achievements",
                    dto.id()));
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
