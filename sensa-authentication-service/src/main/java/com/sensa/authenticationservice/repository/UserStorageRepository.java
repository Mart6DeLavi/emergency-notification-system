package com.sensa.authenticationservice.repository;

import com.sensa.authenticationservice.entity.AuthEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserStorageRepository extends JpaRepository<AuthEntity, Long> {
}
