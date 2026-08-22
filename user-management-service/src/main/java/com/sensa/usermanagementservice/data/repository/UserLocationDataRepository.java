package com.sensa.usermanagementservice.data.repository;

import com.sensa.usermanagementservice.data.entity.UserLocationDataEntity;
import com.sensa.usermanagementservice.dto.UserLocationResponse;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserLocationDataRepository extends ReactiveCrudRepository<UserLocationDataEntity, Long> {

    @Query("SELECT user_entity_id, country, city, street, home_number, created_at, updated_at FROM user_location_data WHERE user_entity_id = :userEntityId")
    Mono<UserLocationDataEntity> findByUserEntityId(Long userEntityId);

    @Query("DELETE FROM user_location_data WHERE user_entity_id = :userEntityId")
    Mono<Integer> deleteByUserEntityId(Long userEntityId);

    @Query("""
            SELECT u.user_id AS user_id,
                   m.email AS email,
                   m.phone_number AS phone_number,
                   m.first_name AS first_name,
                   m.last_name AS last_name,
                   n.push AS push,
                   n.email_enabled AS email_enabled,
                   n.sms AS sms
            FROM user_location_data l
            JOIN users u ON u.id = l.user_entity_id
            JOIN user_main_data m ON m.user_entity_id = u.id
            JOIN user_notification_settings n ON n.user_entity_id = u.id
            WHERE LOWER(l.city) = LOWER(:city)
              AND LOWER(l.street) = LOWER(:street)
            """)
    Flux<UserLocationResponse> findUsersByCityAndStreet(String city, String street);

    @Query("""
            SELECT u.user_id AS user_id,
                   m.email AS email,
                   m.phone_number AS phone_number,
                   m.first_name AS first_name,
                   m.last_name AS last_name,
                   n.push AS push,
                   n.email_enabled AS email_enabled,
                   n.sms AS sms
            FROM user_location_data l
            JOIN users u ON u.id = l.user_entity_id
            JOIN user_main_data m ON m.user_entity_id = u.id
            JOIN user_notification_settings n ON n.user_entity_id = u.id
            WHERE LOWER(l.city) = LOWER(:city)
            """)
    Flux<UserLocationResponse> findUsersByCity(String city);
}
