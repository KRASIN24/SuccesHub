package com.succeshub.appdomain.repository;

import com.succeshub.appdomain.model.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepository extends JpaRepository<Goal, UUID> {
    List<Goal> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, Goal.Status status);
    Optional<Goal> findByIdAndUserId(UUID id, String userId);

    @Query("SELECT COUNT(g) FROM Goal g WHERE g.userId = :userId")
    long countTotal(@Param("userId") String userId);

    @Query("SELECT COUNT(g) FROM Goal g WHERE g.userId = :userId AND g.status = 'COMPLETED'")
    long countCompleted(@Param("userId") String userId);
}
