package com.succeshub.appdomain.controller;

import com.succeshub.appdomain.dto.TaskDto;
import com.succeshub.appdomain.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService service;

    @GetMapping
    public ResponseEntity<List<TaskDto.Response>> getTasks(
            @AuthenticationPrincipal OidcUser principal,
            @RequestParam(required = false) String status) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        if (status != null && !status.isBlank()) {
            try {
                com.succeshub.appdomain.model.Task.Status taskStatus =
                        com.succeshub.appdomain.model.Task.Status.valueOf(status.toUpperCase());
                return ResponseEntity.ok(service.getTasksByStatus(principal.getSubject(), taskStatus));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok(service.getTasks(principal.getSubject()));
    }

    @PostMapping
    public ResponseEntity<TaskDto.Response> createTask(
            @AuthenticationPrincipal OidcUser principal,
            @Valid @RequestBody TaskDto.CreateRequest req) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.create(principal.getSubject(), req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskDto.Response> updateTask(
            @AuthenticationPrincipal OidcUser principal,
            @PathVariable UUID id,
            @Valid @RequestBody TaskDto.UpdateRequest req) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.update(principal.getSubject(), id, req));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<TaskDto.Response> completeTask(
            @AuthenticationPrincipal OidcUser principal,
            @PathVariable UUID id) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(service.complete(principal.getSubject(), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @AuthenticationPrincipal OidcUser principal,
            @PathVariable UUID id) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        service.delete(principal.getSubject(), id);
        return ResponseEntity.noContent().build();
    }
}
