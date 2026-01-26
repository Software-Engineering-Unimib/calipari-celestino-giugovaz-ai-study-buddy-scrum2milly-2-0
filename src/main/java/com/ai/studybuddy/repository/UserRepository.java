package com.ai.studybuddy.repository;

import com.ai.studybuddy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    /**
     * Trova utente per email
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Verifica se esiste un utente con questa email
     */
    boolean existsByEmail(String email);
}