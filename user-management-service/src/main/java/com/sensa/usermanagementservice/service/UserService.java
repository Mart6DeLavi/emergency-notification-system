package com.sensa.usermanagementservice.service;

import com.sensa.usermanagementservice.data.entity.UserEntity;
import com.sensa.usermanagementservice.data.entity.UserLocationDataEntity;
import com.sensa.usermanagementservice.data.entity.UserMainDataEntity;
import com.sensa.usermanagementservice.data.entity.UserNotificationsSettingsEntity;
import com.sensa.usermanagementservice.data.repository.UserEntityRepository;
import com.sensa.usermanagementservice.data.repository.UserLocationDataRepository;
import com.sensa.usermanagementservice.data.repository.UserMainDataRepository;
import com.sensa.usermanagementservice.data.repository.UserNotificationSettingsRepository;
import com.sensa.usermanagementservice.dto.UserCreateEvent;
import com.sensa.usermanagementservice.dto.UserLocationResponse;
import com.sensa.usermanagementservice.dto.UserResponse;
import com.sensa.usermanagementservice.dto.UserUpdateRequest;
import com.sensa.usermanagementservice.exception.UserNotFoundException;
import com.sensa.usermanagementservice.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import io.r2dbc.spi.Row;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserEntityRepository userEntityRepository;
    private final UserMainDataRepository userMainDataRepository;
    private final UserLocationDataRepository userLocationDataRepository;
    private final UserNotificationSettingsRepository userNotificationSettingsRepository;
    private final UserMapper userMapper;
    private final DatabaseClient databaseClient;

    public Mono<Void> createFromKafkaEvent(UserCreateEvent event) {
        log.info("Creating user from Kafka event: userId={}", event.userId());

        LocalDateTime now = LocalDateTime.now();

        return databaseClient.sql("""
                        INSERT INTO users (user_id, created_at)
                        VALUES (:userId, :now)
                        RETURNING id
                        """)
                .bind("userId", event.userId())
                .bind("now", now)
                .map((row, metadata) -> row.get("id", Long.class))
                .one()
                .flatMap(id -> Mono.when(
                        insertMainData(id, event, now),
                        insertLocation(id, event, now),
                        insertNotificationSettings(id, now)
                ))
                .doOnSuccess(v -> log.info("User created successfully: userId={}", event.userId()))
                .doOnError(e -> log.error("Failed to create user: userId={}", event.userId(), e))
                .then();
    }

    private Mono<Void> insertMainData(Long id, UserCreateEvent event, LocalDateTime now) {
        return databaseClient.sql("""
                        INSERT INTO user_main_data
                            (user_entity_id, first_name, last_name, email, phone_number, created_at, updated_at)
                        VALUES (:id, :firstName, :lastName, :email, :phoneNumber, :now, :now)
                        """)
                .bind("id", id)
                .bind("firstName", event.firstName())
                .bind("lastName", event.lastName())
                .bind("email", event.email())
                .bind("phoneNumber", event.phoneNumber())
                .bind("now", now)
                .then();
    }

    private Mono<Void> insertLocation(Long id, UserCreateEvent event, LocalDateTime now) {
        return databaseClient.sql("""
                        INSERT INTO user_location_data
                            (user_entity_id, country, city, street, home_number, created_at, updated_at)
                        VALUES (:id, :country, :city, :street, :homeNumber, :now, :now)
                        """)
                .bind("id", id)
                .bind("country", event.country())
                .bind("city", event.city())
                .bind("street", event.street())
                .bind("homeNumber", event.homeNumber())
                .bind("now", now)
                .then();
    }

    private Mono<Void> insertNotificationSettings(Long id, LocalDateTime now) {
        return databaseClient.sql("""
                        INSERT INTO user_notification_settings
                            (user_entity_id, push, email_enabled, sms, created_at, updated_at)
                        VALUES (:id, TRUE, TRUE, TRUE, :now, :now)
                        """)
                .bind("id", id)
                .bind("now", now)
                .then();
    }

    public Mono<UserResponse> getByUserId(UUID userId) {
        log.info("Fetching user by userId={}", userId);

        return userEntityRepository.findByUserId(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found: " + userId)))
                .flatMap(entity -> Mono.zip(
                        Mono.just(entity),
                        userMainDataRepository.findByUserEntityId(entity.getId()),
                        userLocationDataRepository.findByUserEntityId(entity.getId()),
                        userNotificationSettingsRepository.findByUserEntityId(entity.getId())
                ))
                .map(tuple -> userMapper.toResponse(tuple.getT1(), tuple.getT2(), tuple.getT3(), tuple.getT4()))
                .doOnSuccess(response -> log.info("User fetched: userId={}", userId))
                .doOnError(e -> log.error("Failed to fetch user: userId={}", userId, e));
    }

    public Mono<UserResponse> update(UUID userId, UserUpdateRequest request) {
        log.info("Updating user: userId={}", userId);

        return userEntityRepository.findByUserId(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found: " + userId)))
                .flatMap(entity -> updateMainData(entity.getId(), request)
                        .then(updateLocation(entity.getId(), request))
                        .then(updateNotificationSettings(entity.getId(), request))
                        .then(updateUserTimestamp(entity))
                )
                .then(getByUserId(userId));
    }

    private Mono<UserMainDataEntity> updateMainData(Long userEntityId, UserUpdateRequest request) {
        return userMainDataRepository.findByUserEntityId(userEntityId)
                .flatMap(existing -> {
                    if (request.firstName() != null) existing.setFirstName(request.firstName());
                    if (request.lastName() != null) existing.setLastName(request.lastName());
                    if (request.email() != null) existing.setEmail(request.email());
                    if (request.phoneNumber() != null) existing.setPhoneNumber(request.phoneNumber());
                    if (request.deviceToken() != null) existing.setDeviceToken(request.deviceToken());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return userMainDataRepository.save(existing);
                });
    }

    private Mono<UserLocationDataEntity> updateLocation(Long userEntityId, UserUpdateRequest request) {
        return userLocationDataRepository.findByUserEntityId(userEntityId)
                .flatMap(existing -> {
                    if (request.country() != null) existing.setCountry(request.country());
                    if (request.city() != null) existing.setCity(request.city());
                    if (request.street() != null) existing.setStreet(request.street());
                    if (request.homeNumber() != null) existing.setHomeNumber(request.homeNumber());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return userLocationDataRepository.save(existing);
                });
    }

    private Mono<UserNotificationsSettingsEntity> updateNotificationSettings(Long userEntityId, UserUpdateRequest request) {
        if (request.notificationSettings() == null) {
            return Mono.empty();
        }
        return userNotificationSettingsRepository.findByUserEntityId(userEntityId)
                .flatMap(existing -> {
                    existing.setPush(request.notificationSettings().push());
                    existing.setEmailEnabled(request.notificationSettings().emailEnabled());
                    existing.setSms(request.notificationSettings().sms());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return userNotificationSettingsRepository.save(existing);
                });
    }

    private Mono<UserEntity> updateUserTimestamp(UserEntity entity) {
        entity.setUpdatedAt(LocalDateTime.now());
        return userEntityRepository.save(entity);
    }

    private static final String RECIPIENT_SELECT = """
            SELECT u.user_id,
                   m.email,
                   m.phone_number,
                   m.first_name,
                   m.last_name,
                   m.device_token,
                   n.push,
                   n.email_enabled,
                   n.sms
            FROM users u
            JOIN user_main_data m ON m.user_entity_id = u.id
            JOIN user_notification_settings n ON n.user_entity_id = u.id
            """;

    public Flux<UserLocationResponse> findUsersByLocation(String city, String street) {
        log.info("Fetching users by location: city={}, street={}", city, street);

        boolean hasStreet = street != null && !street.isBlank();
        String sql = RECIPIENT_SELECT
                + " JOIN user_location_data l ON l.user_entity_id = u.id"
                + " WHERE LOWER(l.city) = LOWER(:city)"
                + (hasStreet ? " AND LOWER(l.street) = LOWER(:street)" : "");

        var spec = databaseClient.sql(sql).bind("city", city);
        if (hasStreet) {
            spec = spec.bind("street", street);
        }
        return spec.map((row, metadata) -> mapRecipient(row)).all();
    }

    public Flux<UserLocationResponse> findAllRecipients() {
        log.info("Fetching all notification recipients");
        return databaseClient.sql(RECIPIENT_SELECT)
                .map((row, metadata) -> mapRecipient(row))
                .all();
    }

    private UserLocationResponse mapRecipient(Row row) {
        return new UserLocationResponse(
                row.get("user_id", UUID.class),
                row.get("email", String.class),
                row.get("phone_number", String.class),
                row.get("first_name", String.class),
                row.get("last_name", String.class),
                row.get("device_token", String.class),
                Boolean.TRUE.equals(row.get("push", Boolean.class)),
                Boolean.TRUE.equals(row.get("email_enabled", Boolean.class)),
                Boolean.TRUE.equals(row.get("sms", Boolean.class))
        );
    }

    public Mono<Void> delete(UUID userId) {
        log.info("Deleting user: userId={}", userId);

        return userEntityRepository.findByUserId(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException("User not found: " + userId)))
                .flatMap(entity ->
                        userNotificationSettingsRepository.deleteByUserEntityId(entity.getId())
                                .then(userLocationDataRepository.deleteByUserEntityId(entity.getId()))
                                .then(userMainDataRepository.deleteByUserEntityId(entity.getId()))
                                .then(userEntityRepository.delete(entity))
                )
                .doOnSuccess(v -> log.info("User deleted: userId={}", userId))
                .doOnError(e -> log.error("Failed to delete user: userId={}", userId, e))
                .then();
    }
}
