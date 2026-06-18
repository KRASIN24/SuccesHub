package com.succeshub.appdomain.repository;

import com.succeshub.appdomain.model.AchievementDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Persistence access for global {@link AchievementDefinition} catalog rows.
 * <p>
 * Inherits standard CRUD from {@link JpaRepository}; no user-scoped queries are defined here
 * because unlock state is stored separately in {@link UserAchievementRepository}.
 */
public interface AchievementDefinitionRepository extends JpaRepository<AchievementDefinition, UUID> {
}
