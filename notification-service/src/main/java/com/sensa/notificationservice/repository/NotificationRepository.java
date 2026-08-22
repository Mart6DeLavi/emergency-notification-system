package com.sensa.notificationservice.repository;

import com.sensa.notificationservice.entity.Notification;
import com.sensa.notificationservice.model.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Notification> findByIdAndUserId(Long id, UUID userId);

    int deleteByIdAndUserId(Long id, UUID userId);

    List<Notification> findByChannel(NotificationChannel channel);
}
