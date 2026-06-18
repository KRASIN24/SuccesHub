package com.succeshub.appdomain.repository;

import com.succeshub.appdomain.model.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence access for per-user {@link UserAchievement} unlock records.
 */
public interface UserAchievementRepository extends JpaRepository<UserAchievement, UUID> {

    /**
     * Returns every achievement unlock row for a user.
     *
     * @param userId Keycloak subject ID of the user
     * @return unlocked achievements for that user
     */
    List<UserAchievement> findByUserId(String userId);

    /**
     * Looks up whether a user has unlocked a specific achievement definition.
     *
     * @param userId        Keycloak subject ID of the user
     * @param achievementId primary key of the achievement definition
     * @return the unlock row when present, otherwise empty
     */
    Optional<UserAchievement> findByUserIdAndAchievementId(String userId, UUID achievementId);
}
