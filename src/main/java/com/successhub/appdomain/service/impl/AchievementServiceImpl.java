package com.successhub.appdomain.service.impl;

import com.successhub.appdomain.dto.AchievementDto;
import com.successhub.appdomain.entity.UserAchievement;
import com.successhub.appdomain.repository.AchievementDefinitionRepository;
import com.successhub.appdomain.repository.UserAchievementRepository;
import com.successhub.appdomain.service.AchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

    private final AchievementDefinitionRepository definitionRepository;
    private final UserAchievementRepository userAchievementRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AchievementDto> getAchievements(String userId) {
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
