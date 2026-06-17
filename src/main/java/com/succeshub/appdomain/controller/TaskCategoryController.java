package com.succeshub.appdomain.controller;

import com.succeshub.appdomain.dto.TaskCategoryDto;
import com.succeshub.appdomain.service.TaskCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/task-categories")
@RequiredArgsConstructor
public class TaskCategoryController {

    private final TaskCategoryService service;

    @GetMapping
    public ResponseEntity<List<TaskCategoryDto.Response>> getCategories(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.getCategories(principal.getSubject()));
    }

    @PostMapping
    public ResponseEntity<TaskCategoryDto.Response> createCategory(
            @AuthenticationPrincipal OidcUser principal,
            @Valid @RequestBody TaskCategoryDto.CreateRequest req) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.create(principal.getSubject(), req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskCategoryDto.Response> updateCategory(
            @AuthenticationPrincipal OidcUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody TaskCategoryDto.UpdateRequest req) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.update(principal.getSubject(), id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @AuthenticationPrincipal OidcUser principal,
            @PathVariable UUID id) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        service.delete(principal.getSubject(), id);
        return ResponseEntity.noContent().build();
    }
}
