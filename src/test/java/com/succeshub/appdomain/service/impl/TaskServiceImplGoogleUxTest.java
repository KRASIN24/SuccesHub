package com.succeshub.appdomain.service.impl;

import com.succeshub.appdomain.dto.TaskDto;
import com.succeshub.appdomain.model.Task;
import com.succeshub.appdomain.model.TaskCategory;
import com.succeshub.appdomain.repository.GoalRepository;
import com.succeshub.appdomain.repository.TaskCategoryRepository;
import com.succeshub.appdomain.repository.TaskRepository;
import com.succeshub.appdomain.repository.XpEventRepository;
import com.succeshub.appdomain.service.GamificationEngine;
import com.succeshub.appdomain.service.UserProfileService;
import com.succeshub.appdomain.service.gamification.GamificationTimeUtil;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplGoogleUxTest {

    @Mock TaskRepository taskRepository;
    @Mock XpEventRepository xpEventRepository;
    @Mock TaskCategoryRepository categoryRepository;
    @Mock GoalRepository goalRepository;
    @Mock UserProfileService profileService;
    @Mock GamificationEngine gamificationEngine;
    @Mock GamificationTimeUtil timeUtil;
    @Mock TaskMapper taskMapper;

    @InjectMocks TaskServiceImpl taskService;

    private UUID categoryId;
    private TaskCategory category;
    private UUID rootId;
    private Task root;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        category = new TaskCategory();
        category.setId(categoryId);
        category.setUserId("user-1");
        category.setName("Inbox");
        category.setGrantXp(true);

        rootId = UUID.randomUUID();
        root = new Task();
        root.setId(rootId);
        root.setUserId("user-1");
        root.setCategory(category);
        root.setTitle("Root");
        root.setStatus(Task.Status.TODO);
    }

    @Test
    void create_rejectsParentThatIsItselfASubtask() {
        UUID nestedParentId = UUID.randomUUID();
        Task mid = new Task();
        mid.setId(nestedParentId);
        mid.setUserId("user-1");
        mid.setCategory(category);
        mid.setParentTask(root);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(taskRepository.findByIdAndUserId(nestedParentId, "user-1")).thenReturn(Optional.of(mid));

        TaskDto.CreateRequest req = new TaskDto.CreateRequest(
                categoryId, null, nestedParentId, false, null,
                "Child", null, 0, 3, 30, 2, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () -> taskService.create("user-1", req));
    }

    @Test
    void create_withValidParent_setsParentAndSortOrder() {
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(taskRepository.findByIdAndUserId(rootId, "user-1")).thenReturn(Optional.of(root));
        when(taskRepository.countByParentTask_Id(rootId)).thenReturn(1L);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
        when(taskMapper.toDto(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            return new TaskDto.Response(
                    t.getId(), categoryId, "Inbox", null, rootId, false, t.getSortOrder(),
                    t.getTitle(), null, 0, 3, 30, 2, false, null, "TODO",
                    null, null, null, null, null, null);
        });

        TaskDto.CreateRequest req = new TaskDto.CreateRequest(
                categoryId, null, rootId, false, null,
                "Sub", null, 0, 3, 30, 2, null, null, null, null);

        TaskDto.Response result = taskService.create("user-1", req);

        assertEquals(rootId, result.parentTaskId());
        assertEquals(1, result.sortOrder());
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertEquals(root, captor.getValue().getParentTask());
        assertEquals(1, captor.getValue().getSortOrder());
    }

    @Test
    void update_rejectsNestingTaskThatAlreadyHasChildren() {
        UUID newParentId = UUID.randomUUID();

        when(taskRepository.findByIdAndUserId(rootId, "user-1")).thenReturn(Optional.of(root));
        when(taskRepository.countByParentTask_Id(rootId)).thenReturn(1L);

        TaskDto.UpdateRequest req = new TaskDto.UpdateRequest(
                categoryId, null, newParentId, false, false, 0,
                "Root", null, 0, 3, 30, 2, "TODO",
                null, null, null, null, false);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> taskService.update("user-1", rootId, req));

        assertEquals("Task with subtasks cannot become a subtask", error.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void setStarred_togglesFlag() {
        when(taskRepository.findByIdAndUserId(rootId, "user-1")).thenReturn(Optional.of(root));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));
        when(taskMapper.toDto(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            return new TaskDto.Response(
                    t.getId(), categoryId, "Inbox", null, null, t.isStarred(), 0,
                    t.getTitle(), null, 0, 3, 30, 2, false, null, "TODO",
                    null, null, null, null, null, null);
        });

        TaskDto.Response result = taskService.setStarred("user-1", rootId, true);

        assertTrue(result.starred());
        assertTrue(root.isStarred());
    }

    @Test
    void reorder_rewritesSortOrderForSiblingGroup() {
        UUID aId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();
        Task a = task(aId, "A", 0);
        Task b = task(bId, "B", 1);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(taskRepository.findByIdAndUserId(aId, "user-1")).thenReturn(Optional.of(a));
        when(taskRepository.findByIdAndUserId(bId, "user-1")).thenReturn(Optional.of(b));
        when(taskRepository.findByUserIdAndCategory_IdAndParentTaskIsNullOrderBySortOrderAsc("user-1", categoryId))
                .thenReturn(List.of(a, b));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        taskService.reorder("user-1", new TaskDto.ReorderRequest(categoryId, null, List.of(bId, aId)));

        assertEquals(0, b.getSortOrder());
        assertEquals(1, a.getSortOrder());
    }

    @Test
    void reorder_rejectsPartialSiblingList() {
        UUID aId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();
        Task a = task(aId, "A", 0);
        Task b = task(bId, "B", 1);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(taskRepository.findByIdAndUserId(aId, "user-1")).thenReturn(Optional.of(a));
        when(taskRepository.findByUserIdAndCategory_IdAndParentTaskIsNullOrderBySortOrderAsc("user-1", categoryId))
                .thenReturn(List.of(a, b));

        assertThrows(IllegalArgumentException.class, () ->
                taskService.reorder("user-1", new TaskDto.ReorderRequest(categoryId, null, List.of(aId))));
    }

    @Test
    void reorder_rejectsForeignTask() {
        UUID foreignId = UUID.randomUUID();
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(taskRepository.findByIdAndUserId(foreignId, "user-1")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                taskService.reorder("user-1", new TaskDto.ReorderRequest(categoryId, null, List.of(foreignId))));
    }

    private Task task(UUID id, String title, int sortOrder) {
        Task t = new Task();
        t.setId(id);
        t.setUserId("user-1");
        t.setCategory(category);
        t.setTitle(title);
        t.setSortOrder(sortOrder);
        t.setStatus(Task.Status.TODO);
        return t;
    }
}
