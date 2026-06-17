package com.succeshub.appdomain.repository;

import com.succeshub.appdomain.model.TaskCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskCategoryRepository extends JpaRepository<TaskCategory, UUID> {
    List<TaskCategory> findByUserIdOrderBySortOrderAsc(String userId);
}
