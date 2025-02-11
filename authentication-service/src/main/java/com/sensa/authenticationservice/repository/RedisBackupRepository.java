package com.sensa.authenticationservice.repository;

import com.sensa.authenticationservice.entity.RedisBackupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RedisBackupRepository extends JpaRepository<RedisBackupEntity, Long> {

    @Query("SELECT r FROM RedisBackupEntity r WHERE r.id IN " +
    "(SELECT MAX(r2.id) FROM RedisBackupEntity r2 WHERE r2.username = r.username AND r2.token IS NOT NULL " +
    "AND r2.id >= :since GROUP BY r2.username)")
    List<RedisBackupEntity> findRecentTokens(@Param("since") Long since);

    List<RedisBackupEntity> findByCreatedAtAfter(Object unknownAttr1);
}
