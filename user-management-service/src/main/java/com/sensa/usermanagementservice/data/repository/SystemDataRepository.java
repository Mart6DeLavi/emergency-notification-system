package com.sensa.usermanagementservice.data.repository;

import com.sensa.usermanagementservice.data.entity.SystemData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface SystemDataRepository extends JpaRepository<SystemData, Long> {

    @Modifying
    @Query("DELETE FROM SystemData s WHERE s.createdAt < :cutoffTime")
    void deleteByCreatedAtBefore(@Param("cutoffTime") LocalDateTime cutoffTime);
}
