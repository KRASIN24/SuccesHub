package com.succeshub.appdomain.controller;

import com.succeshub.appdomain.dto.NotificationDto;
import com.succeshub.appdomain.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST API for the in-app notification inbox shown in the navbar bell.
 */
@Tag(name = "Notifications", description = "In-app notification inbox (no email/push)")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Lists the authenticated user's newest notifications.
     */
    @Operation(summary = "List notifications", description = "Returns the newest notifications for the logged-in user.")
    @ApiResponse(responseCode = "200", description = "List returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @GetMapping
    public ResponseEntity<List<NotificationDto.Response>> list(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Max rows (1–50, default 20)")
            @RequestParam(defaultValue = "20") int limit) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(notificationService.listRecent(principal.getSubject(), limit));
    }

    /**
     * Returns the unread badge count for the navbar.
     */
    @Operation(summary = "Unread count", description = "Returns how many unread notifications the user has.")
    @ApiResponse(responseCode = "200", description = "Count returned")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @GetMapping("/unread-count")
    public ResponseEntity<NotificationDto.UnreadCount> unreadCount(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(notificationService.unreadCount(principal.getSubject()));
    }

    /**
     * Marks a single notification as read.
     */
    @Operation(summary = "Mark one read", description = "Marks the given notification as read when owned by the caller.")
    @ApiResponse(responseCode = "200", description = "Notification updated")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @ApiResponse(responseCode = "404", description = "Notification not found")
    @PostMapping("/{id}/read")
    public ResponseEntity<NotificationDto.Response> markRead(
            @AuthenticationPrincipal OidcUser principal,
            @Parameter(description = "Notification ID") @PathVariable UUID id) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(notificationService.markRead(principal.getSubject(), id));
    }

    /**
     * Marks every unread notification as read.
     */
    @Operation(summary = "Mark all read", description = "Marks all unread notifications for the logged-in user as read.")
    @ApiResponse(responseCode = "200", description = "Inbox cleared")
    @ApiResponse(responseCode = "401", description = "Not authenticated")
    @PostMapping("/read-all")
    public ResponseEntity<NotificationDto.UnreadCount> markAllRead(@AuthenticationPrincipal OidcUser principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(notificationService.markAllRead(principal.getSubject()));
    }
}
