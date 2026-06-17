package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.AchievementDto;
import com.succeshub.appdomain.model.AchievementDefinition;
import com.succeshub.appdomain.model.UserAchievement;
import com.succeshub.appdomain.repository.AchievementDefinitionRepository;
import com.succeshub.appdomain.repository.UserAchievementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementDefinitionRepository definitionRepository;
    private final UserAchievementRepository userAchievementRepository;

    @Transactional(readOnly = true)
    public List<AchievementDto> getAchievements(String userId) {
        // Collect IDs of achievements this user has unlocked
        Set<UUID> unlockedIds = userAchievementRepository.findByUserId(userId).stream()
                .map(ua -> ua.getAchievement().getId())
                .collect(Collectors.toSet());

        return definitionRepository.findAll().stream()
                .map(def -> {
                    UserAchievement ua = userAchievementRepository
                            .findByUserIdAndAchievementId(userId, def.getId())
                            .orElse(null);
                    boolean locked = !unlockedIds.contains(def.getId());
                    return new AchievementDto(
                            def.getId(),
                            def.getKey(),
                            def.getLabel(),
                            def.getIcon(),
                            def.getDescription(),
                            locked,
                            ua != null ? ua.getUnlockedAt() : null
                    );
                })
                .toList();
    }
}
