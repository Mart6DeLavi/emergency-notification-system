package com.sensa.usermanagementservice.data.repository;

import com.sensa.usermanagementservice.data.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    @Query("SELECT c FROM client c WHERE c.username = :username")
    Optional<Client> findClientByUsername(@Param("username") final String username);

    @Query("SELECT c FROM client c")
    List<Client> getAllClients();

    @Query("SELECT c FROM client c WHERE c.email = :email")
    Optional<Client> findClientByEmail(@Param("email") final String email);
}
