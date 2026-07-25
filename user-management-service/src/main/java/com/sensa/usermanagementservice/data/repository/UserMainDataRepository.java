package com.sensa.usermanagementservice.data.repository;

import com.sensa.usermanagementservice.data.entity.UserMainDataEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserMainDataRepository extends ReactiveCrudRepository<UserMainDataEntity, Long> {

    @Query("SELECT user_entity_id, first_name, last_name, email, phone_number, created_at, updated_at FROM user_main_data WHERE user_entity_id = :userEntityId")
    Mono<UserMainDataEntity> findByUserEntityId(Long userEntityId);

    @Query("DELETE FROM user_main_data WHERE user_entity_id = :userEntityId")
    Mono<Integer> deleteByUserEntityId(Long userEntityId);
}
