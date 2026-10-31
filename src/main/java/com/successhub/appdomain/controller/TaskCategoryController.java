package com.successhub.appdomain.controller;

import com.successhub.appdomain.dto.TaskCategoryDto;
import com.successhub.appdomain.service.TaskCategoryService;
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
 * REST API for Kanban columns on the Strategy Canvas, including the grant-XP toggle.
 */
@Tag(name = "Task Categories", description = "Kanban column CRUD")
@RestController
@RequestMapping("/api/task-categories")
@RequiredArgsConstructor
public class TaskCategoryController {

    private final TaskCategoryService service;

    /**
     * Lists all task category columns for the authenticated user.
     */
    @Operation(summary = "List task categories")
    @ApiResponse(responseCode = "200", description = "Categories returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @GetMapping
    public ResponseEntity<List<TaskCategoryDto.Response>> getCategories(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.getCategories(principal.getSubject()));
    }

    /**
     * Creates a new task category column.
     */
    @Operation(summary = "Create task category")
    @ApiResponse(responseCode = "200", description = "Category created")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @PostMapping
    public ResponseEntity<TaskCategoryDto.Response> createCategory(
            @AuthenticationPrincipal OidcUser principal,
            @Valid @RequestBody TaskCategoryDto.CreateRequest req) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.create(principal.getSubject(), req));
    }

    /**
     * Updates an existing task category.
     */
    @Operation(summary = "Update task category")
    @ApiResponse(responseCode = "200", description = "Category updated")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Category not found")
    @PutMapping("/{id}")
    public ResponseEntity<TaskCategoryDto.Response> updateCategory(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Category ID") @PathVariable UUID id,
            @Valid @RequestBody TaskCategoryDto.UpdateRequest req) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.update(principal.getSubject(), id, req));
    }

    /**
     * Deletes a task category owned by the authenticated user.
     */
    @Operation(summary = "Delete task category")
    @ApiResponse(responseCode = "204", description = "Category deleted")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Category not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Category ID") @PathVariable UUID id) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        service.delete(principal.getSubject(), id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Rewrites sort_order for all of the user's category columns.
     */
    @Operation(summary = "Reorder task categories", description = "Sets sort_order 0..n for orderedIds (must list every category).")
    @ApiResponse(responseCode = "204", description = "Order updated")
    @ApiResponse(responseCode = "400", description = "Invalid orderedIds")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @PatchMapping("/reorder")
    public ResponseEntity<Void> reorderCategories(
            @AuthenticationPrincipal OidcUser principal,
            @Valid @RequestBody TaskCategoryDto.ReorderRequest req) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        service.reorder(principal.getSubject(), req);
        return ResponseEntity.noContent().build();
    }
}
