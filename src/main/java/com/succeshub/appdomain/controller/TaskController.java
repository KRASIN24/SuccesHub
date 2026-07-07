package com.succeshub.appdomain.controller;

import com.succeshub.appdomain.dto.TaskDto;
import com.succeshub.appdomain.dto.gamification.GamificationDto.TaskCompletionDto;
import com.succeshub.appdomain.model.Task;
import com.succeshub.appdomain.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for protocol tasks on the dashboard and Strategy Canvas.
 */
@Tag(name = "Tasks", description = "User task CRUD and completion")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService service;

    /**
     * Lists tasks for the authenticated user, optionally filtered by status.
     */
    @Operation(summary = "List tasks", description = "Returns all tasks for the user, or only those matching the optional status filter.")
    @ApiResponse(responseCode = "200", description = "Tasks returned")
    @ApiResponse(responseCode = "400", description = "Invalid status value")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @GetMapping
    public ResponseEntity<List<TaskDto.Response>> getTasks(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Filter by status: TODO, IN_PROGRESS, or DONE")
            @RequestParam(required = false) String status) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        if (status != null && !status.isBlank()) {
            try {
                Task.Status taskStatus = Task.Status.valueOf(status.toUpperCase());
                return ResponseEntity.ok(service.getTasksByStatus(principal.getSubject(), taskStatus));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok(service.getTasks(principal.getSubject()));
    }

    /**
     * Creates a new task for the authenticated user.
     */
    @Operation(summary = "Create task")
    @ApiResponse(responseCode = "200", description = "Task created")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @PostMapping
    public ResponseEntity<TaskDto.Response> createTask(
            @AuthenticationPrincipal OidcUser principal,
            @Valid @RequestBody TaskDto.CreateRequest req) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.create(principal.getSubject(), req));
    }

    /**
     * Updates an existing task owned by the authenticated user.
     */
    @Operation(summary = "Update task")
    @ApiResponse(responseCode = "200", description = "Task updated")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Task not found")
    @PutMapping("/{id}")
    public ResponseEntity<TaskDto.Response> updateTask(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Task ID") @PathVariable UUID id,
            @Valid @RequestBody TaskDto.UpdateRequest req) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.update(principal.getSubject(), id, req));
    }

    /**
     * Marks a task as done and runs the gamification reward engine when the category grants XP.
     */
    @Operation(summary = "Complete task", description = "Sets status to DONE, awards calculated XP, boss damage, and achievements. Idempotent when already completed or XP was previously awarded for this task.")
    @ApiResponse(responseCode = "200", description = "Task completed with reward payload")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Task not found")
    @PatchMapping("/{id}/complete")
    public ResponseEntity<TaskCompletionDto> completeTask(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Task ID") @PathVariable UUID id) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.complete(principal.getSubject(), id));
    }

    /**
     * Commits selected tasks to today's daily ritual schedule.
     */
    @Operation(summary = "Schedule tasks for today", description = "Sets scheduled_date to today for the given task IDs.")
    @ApiResponse(responseCode = "204", description = "Tasks scheduled")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Task not found")
    @PatchMapping("/schedule")
    public ResponseEntity<Void> scheduleTasks(
            @AuthenticationPrincipal OidcUser principal,
            @Valid @RequestBody TaskDto.ScheduleRequest req) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        service.scheduleTasks(principal.getSubject(), req.taskIds());
        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes a task owned by the authenticated user.
     */
    @Operation(summary = "Delete task")
    @ApiResponse(responseCode = "204", description = "Task deleted")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Task not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Task ID") @PathVariable UUID id) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        service.delete(principal.getSubject(), id);
        return ResponseEntity.noContent().build();
    }
}
