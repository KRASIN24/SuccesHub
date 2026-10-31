package com.successhub.coreinfra.account;

import com.successhub.appdomain.repository.GoalRepository;
import com.successhub.appdomain.repository.LootBoxContentRepository;
import com.successhub.appdomain.repository.NotificationRepository;
import com.successhub.appdomain.repository.StreakDayOverrideRepository;
import com.successhub.appdomain.repository.TaskCategoryRepository;
import com.successhub.appdomain.repository.TaskRepository;
import com.successhub.appdomain.repository.UserAchievementRepository;
import com.successhub.appdomain.repository.UserInventoryRepository;
import com.successhub.appdomain.repository.UserLootBoxRepository;
import com.successhub.appdomain.repository.UserProfileRepository;
import com.successhub.appdomain.repository.XpEventRepository;
import com.successhub.coreinfra.account.UserDataCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes user-owned rows in FK-safe order for account self-service deletion.
 */
@Service
@RequiredArgsConstructor
public class UserDataCleanupServiceImpl implements UserDataCleanupService {

    private final TaskRepository taskRepository;
    private final GoalRepository goalRepository;
    private final TaskCategoryRepository taskCategoryRepository;
    private final XpEventRepository xpEventRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final LootBoxContentRepository lootBoxContentRepository;
    private final UserLootBoxRepository userLootBoxRepository;
    private final UserInventoryRepository userInventoryRepository;
    private final StreakDayOverrideRepository streakDayOverrideRepository;
    private final NotificationRepository notificationRepository;
    private final UserProfileRepository userProfileRepository;

    @Override
    @Transactional
    public void deleteAllForUser(String keycloakUserId) {
        // Tasks reference goals / categories — clear tasks first.
        taskRepository.deleteByUserId(keycloakUserId);
        goalRepository.deleteByUserId(keycloakUserId);
        taskCategoryRepository.deleteByUserId(keycloakUserId);

        xpEventRepository.deleteByUserId(keycloakUserId);
        userAchievementRepository.deleteByUserId(keycloakUserId);

        lootBoxContentRepository.deleteByLootBoxUserId(keycloakUserId);
        userLootBoxRepository.deleteByUserId(keycloakUserId);
        userInventoryRepository.deleteByUserId(keycloakUserId);

        streakDayOverrideRepository.deleteByUserId(keycloakUserId);
        notificationRepository.deleteByUserId(keycloakUserId);
        userProfileRepository.deleteByKeycloakId(keycloakUserId);
    }
}
