package com.sensa.notificationservice.repository;

import com.sensa.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query(""" 
            SELECT n 
            FROM Notification n 
            WHERE 
            n.clientUsername = :clientUsername 
    """)
    Optional<Notification> findByClientUsername(@Param("clientUsername") String clientUsername);

    @Query("""
            SELECT n 
            FROM Notification n
            WHERE
            n.title = :title
    """)
    Optional<Notification> findByTitle(@Param("title") String title);

    @Query("""
            SELECT n
            FROM Notification n
            WHERE
            n.clientUsername = :clientUsername
            AND
            n.title = :title
""")
    Optional<Notification> findByClientUsernameAndTitle(@Param("clientUsername") String clientUsername, @Param("title") String title);
}
