package com.sensa.authenticationservice.repository;

import com.sensa.authenticationservice.entity.RedisBackupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RedisBackupRepository extends JpaRepository<RedisBackupEntity, Long> {

}
