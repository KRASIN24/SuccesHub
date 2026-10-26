package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.dto.TaskCategoryDto;
import com.succeshub.appdomain.model.TaskCategory;
import com.succeshub.appdomain.repository.TaskCategoryRepository;
import com.succeshub.appdomain.service.TaskCategoryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskCategoryServiceImpl implements TaskCategoryService {

    private final TaskCategoryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<TaskCategoryDto.Response> getCategories(String userId) {
        return repository.findByUserIdOrderBySortOrderAsc(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public TaskCategoryDto.Response create(String userId, TaskCategoryDto.CreateRequest req) {
        TaskCategory cat = new TaskCategory();
        cat.setUserId(userId);
        cat.setName(req.name());
        cat.setTag(req.tag());
        cat.setGrantXp(req.grantXp());
        cat.setSortOrder(req.sortOrder());
        return toDto(repository.save(cat));
    }

    @Override
    @Transactional
    public TaskCategoryDto.Response update(String userId, UUID id, TaskCategoryDto.UpdateRequest req) {
        TaskCategory cat = repository.findById(id)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + id));
        cat.setName(req.name());
        cat.setTag(req.tag());
        cat.setGrantXp(req.grantXp());
        cat.setSortOrder(req.sortOrder());
        return toDto(repository.save(cat));
    }

    @Override
    @Transactional
    public void delete(String userId, UUID id) {
        TaskCategory cat = repository.findById(id)
                .filter(c -> c.getUserId().equals(userId))
                .orElseThrow(() -> new EntityNotFoundException("Category not found: " + id));
        repository.delete(cat);
    }

    @Override
    @Transactional
    public void reorder(String userId, TaskCategoryDto.ReorderRequest req) {
        List<UUID> orderedIds = req.orderedIds();
        List<TaskCategory> owned = repository.findByUserIdOrderBySortOrderAsc(userId);
        Set<UUID> expected = owned.stream().map(TaskCategory::getId).collect(Collectors.toSet());
        Map<UUID, TaskCategory> byId = owned.stream()
                .collect(Collectors.toMap(TaskCategory::getId, Function.identity()));

        if (orderedIds.size() != new HashSet<>(orderedIds).size()) {
            throw new IllegalArgumentException("orderedIds contains duplicates");
        }
        if (!expected.equals(new HashSet<>(orderedIds))) {
            throw new IllegalArgumentException("orderedIds must list exactly the user's categories");
        }

        for (int i = 0; i < orderedIds.size(); i++) {
            TaskCategory cat = byId.get(orderedIds.get(i));
            cat.setSortOrder(i);
            repository.save(cat);
        }
    }

    private TaskCategoryDto.Response toDto(TaskCategory c) {
        return new TaskCategoryDto.Response(c.getId(), c.getName(), c.getTag(),
                c.isGrantXp(), c.getSortOrder());
    }
}
