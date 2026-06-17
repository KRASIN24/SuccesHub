package com.succeshub.appdomain.repository;

import com.succeshub.appdomain.model.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, UUID> {
    List<UserAchievement> findByUserId(String userId);
    Optional<UserAchievement> findByUserIdAndAchievementId(String userId, UUID achievementId);
}
