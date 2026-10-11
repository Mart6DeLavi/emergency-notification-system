package com.sensa.notificationservice.repository;

import com.sensa.notificationservice.entity.Notification;
import com.sensa.notificationservice.model.NotificationChannel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Notification> findByIdAndUserId(String id, UUID userId);

    long deleteByIdAndUserId(String id, UUID userId);

    List<Notification> findByChannel(NotificationChannel channel);
}
