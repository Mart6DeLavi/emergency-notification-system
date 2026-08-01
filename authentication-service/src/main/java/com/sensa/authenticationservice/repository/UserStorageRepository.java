package com.sensa.authenticationservice.repository;

import com.sensa.authenticationservice.entity.AuthEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserStorageRepository extends JpaRepository<AuthEntity, Long> {

    Optional<AuthEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<AuthEntity> findByUserId(UUID userId);
}
