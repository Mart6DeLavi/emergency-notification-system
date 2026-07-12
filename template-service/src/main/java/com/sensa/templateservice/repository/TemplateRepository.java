package com.sensa.templateservice.repository;

import com.sensa.templateservice.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<Template, Long> {

    @Query(""" 
            SELECT COUNT(t) > 0
            FROM Template t
            WHERE
            t.clientUsername = :username
            AND
            t.title = :title""")
    boolean existTemplateByUsernameAndTitle(String username, String title);

    @Query("""
            SELECT t 
            FROM Template t 
            WHERE 
            t.title = :title 
            AND 
            t.clientUsername = :clientUsername""")
    Optional<Template> findByClientUsernameAndTitle(String clientUsername, String title);

    @Transactional
    @Modifying
    @Query("""
           DELETE 
           FROM Template t 
           WHERE 
           t.title = :title 
           AND 
           t.clientUsername = :clientUsername""")
    int deleteByClientUsernameAndTitle(String clientUsername, String title);
}
