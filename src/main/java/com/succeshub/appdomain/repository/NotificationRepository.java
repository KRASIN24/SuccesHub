package com.succeshub.appdomain.repository;

import com.succeshub.appdomain.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Per-user in-app notification persistence for the navbar bell.
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Returns recent notifications for a user, newest first.
     *
     * @param userId   Keycloak subject ID
     * @param pageable page size / limit
     * @return matching rows
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * Counts unread notifications for a user.
     *
     * @param userId Keycloak subject ID
     * @param read   typically {@code false}
     * @return unread count
     */
    long countByUserIdAndRead(String userId, boolean read);

    /**
     * Finds a notification owned by the given user.
     *
     * @param id     notification ID
     * @param userId expected owner
     * @return row when found and owned
     */
    Optional<Notification> findByIdAndUserId(UUID id, String userId);

    /**
     * Marks every unread notification for the user as read.
     *
     * @param userId Keycloak subject ID
     * @return number of rows updated
     */
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.read = true where n.userId = :userId and n.read = false")
    int markAllRead(@Param("userId") String userId);
}
