package com.example.jari.notification.repository;

import com.example.jari.notification.entity.Notification;
import com.example.jari.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findTop50ByRecipientIdOrderByCreatedAtDesc(UUID recipientId);

    long countByRecipientIdAndReadAtIsNull(UUID recipientId);

    Optional<Notification> findByIdAndRecipientId(UUID id, UUID recipientId);

    @Modifying
    @Query("update Notification n set n.readAt = :readAt " +
           "where n.recipient.id = :recipientId and n.readAt is null")
    int markAllReadByRecipientId(@Param("recipientId") UUID recipientId, @Param("readAt") OffsetDateTime readAt);

    boolean existsByTypeAndIssueIdAndRecipientIdAndCreatedAtGreaterThanEqual(
        NotificationType type, UUID issueId, UUID recipientId, OffsetDateTime since);
}
