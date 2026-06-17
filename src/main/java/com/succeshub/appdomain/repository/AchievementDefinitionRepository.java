package com.succeshub.appdomain.repository;

import com.succeshub.appdomain.model.AchievementDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AchievementDefinitionRepository extends JpaRepository<AchievementDefinition, UUID> {
}
