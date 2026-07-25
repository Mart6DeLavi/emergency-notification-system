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
import com.sensa.usermanagementservice.dto.UserResponse;
import com.sensa.usermanagementservice.dto.UserUpdateRequest;
import com.sensa.usermanagementservice.exception.UserNotFoundException;
import com.sensa.usermanagementservice.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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

    public Mono<Void> createFromKafkaEvent(UserCreateEvent event) {
        log.info("Creating user from Kafka event: userId={}", event.userId());

        UserEntity userEntity = UserEntity.builder()
                .userId(event.userId())
                .build();

        return userEntityRepository.save(userEntity)
                .flatMap(savedEntity -> {
                    UserMainDataEntity mainData = UserMainDataEntity.builder()
                            .userEntityId(savedEntity.getId())
                            .firstName(event.firstName())
                            .lastName(event.lastName())
                            .email(event.email())
                            .phoneNumber(event.phoneNumber())
                            .build();

                    UserLocationDataEntity location = UserLocationDataEntity.builder()
                            .userEntityId(savedEntity.getId())
                            .country(event.country())
                            .city(event.city())
                            .street(event.street())
                            .homeNumber(event.homeNumber())
                            .build();

                    UserNotificationsSettingsEntity settings = UserNotificationsSettingsEntity.builder()
                            .userEntityId(savedEntity.getId())
                            .build();

                    return Mono.when(
                            userMainDataRepository.save(mainData),
                            userLocationDataRepository.save(location),
                            userNotificationSettingsRepository.save(settings)
                    );
                })
                .doOnSuccess(v -> log.info("User created successfully: userId={}", event.userId()))
                .doOnError(e -> log.error("Failed to create user: userId={}", event.userId(), e))
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
