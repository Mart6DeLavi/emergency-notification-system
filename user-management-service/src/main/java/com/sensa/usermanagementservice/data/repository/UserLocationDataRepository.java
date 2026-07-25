package com.sensa.usermanagementservice.data.repository;

import com.sensa.usermanagementservice.data.entity.UserLocationDataEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserLocationDataRepository extends ReactiveCrudRepository<UserLocationDataEntity, Long> {

    @Query("SELECT user_entity_id, country, city, street, home_number, created_at, updated_at FROM user_location_data WHERE user_entity_id = :userEntityId")
    Mono<UserLocationDataEntity> findByUserEntityId(Long userEntityId);

    @Query("DELETE FROM user_location_data WHERE user_entity_id = :userEntityId")
    Mono<Integer> deleteByUserEntityId(Long userEntityId);
}
