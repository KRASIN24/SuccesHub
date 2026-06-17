package com.succeshub.appdomain.service;

import com.succeshub.appdomain.dto.TaskCategoryDto;
import com.succeshub.appdomain.model.TaskCategory;
import com.succeshub.appdomain.repository.TaskCategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskCategoryService {

    private final TaskCategoryRepository repository;

    @Transactional(readOnly = true)
    public List<TaskCategoryDto.Response> getCategories(String userId) {
        return repository.findByUserIdOrderBySortOrderAsc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public TaskCategoryDto.Response create(String userId, TaskCategoryDto.CreateRequest req) {
        TaskCategory cat = new TaskCategory();
        cat.setUserId(userId);
        cat.setName(req.name());
        cat.setTag(req.tag());
        cat.setGrantXp(req.grantXp());
        cat.setMuted(req.muted());
        cat.setSortOrder(req.sortOrder());
        return toDto(repository.save(cat));
    }

    @Transactional
    public TaskCategoryDto.Response update(String userId, UUID id, TaskCategoryDto.UpdateRequest req) {
        TaskCategory cat = repository.findById(id)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + id));
        cat.setName(req.name());
        cat.setTag(req.tag());
        cat.setGrantXp(req.grantXp());
        cat.setMuted(req.muted());
        cat.setSortOrder(req.sortOrder());
        return toDto(repository.save(cat));
    }

    @Transactional
    public void delete(String userId, UUID id) {
        TaskCategory cat = repository.findById(id)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + id));
        repository.delete(cat);
    }

    private TaskCategoryDto.Response toDto(TaskCategory c) {
        return new TaskCategoryDto.Response(c.getId(), c.getName(), c.getTag(),
                c.isGrantXp(), c.isMuted(), c.getSortOrder());
    }
}
