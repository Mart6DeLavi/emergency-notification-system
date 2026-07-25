package com.sensa.usermanagementservice.data.repository;

import com.sensa.usermanagementservice.data.entity.UserNotificationsSettingsEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserNotificationSettingsRepository extends ReactiveCrudRepository<UserNotificationsSettingsEntity, Long> {

    @Query("SELECT user_entity_id, push, email_enabled, sms, created_at, updated_at FROM user_notification_settings WHERE user_entity_id = :userEntityId")
    Mono<UserNotificationsSettingsEntity> findByUserEntityId(Long userEntityId);

    @Query("DELETE FROM user_notification_settings WHERE user_entity_id = :userEntityId")
    Mono<Integer> deleteByUserEntityId(Long userEntityId);
}
