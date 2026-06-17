package com.succeshub.appdomain.repository;

import com.succeshub.appdomain.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Task> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, Task.Status status);
    Optional<Task> findByIdAndUserId(UUID id, String userId);
    long countByUserIdAndStatus(String userId, Task.Status status);
}
