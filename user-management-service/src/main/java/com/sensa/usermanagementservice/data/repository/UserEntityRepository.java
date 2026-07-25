package com.sensa.usermanagementservice.data.repository;

import com.sensa.usermanagementservice.data.entity.UserEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface UserEntityRepository extends ReactiveCrudRepository<UserEntity, Long> {

    @Query("SELECT id, user_id, created_at, updated_at FROM users WHERE user_id = :userId")
    Mono<UserEntity> findByUserId(UUID userId);

    @Query("DELETE FROM users WHERE user_id = :userId")
    Mono<Integer> deleteByUserId(UUID userId);
}
