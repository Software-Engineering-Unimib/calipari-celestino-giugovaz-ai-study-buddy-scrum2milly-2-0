package com.ai.studybuddy.service.inter;

import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.util.enums.EducationLevel;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserService {

    // ==================== CRUD BASE ====================

    List<User> getAllUsers();

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    User save(User user);

    void deleteById(UUID id);

    // ==================== REGISTRAZIONE UNIFICATA ====================

    /**
     * Registra un nuovo utente con EducationLevel (versione legacy)
     */
    User registerUser(String firstName, String lastName, String email, String password, EducationLevel educationLevel);

    /**
     * Registra un nuovo utente con lingua preferita (versione multilingua)
     */
    User registerUser(String firstName, String lastName, String email, String password, String preferredLanguage);

    /**
     * Registra un nuovo utente con TUTTI i parametri (versione completa unificata)
     */
    User registerUser(String firstName, String lastName, String email, String password, 
                     EducationLevel educationLevel, String preferredLanguage);

    // ==================== UTILITY ====================

    boolean existsByEmail(String email);

    User getCurrentUser(Principal principal);

    User updateProfile(Principal principal, String firstName, String lastName, String avatarUrl);

    // ==================== GAMIFICATION ====================

    void addPoints(UUID userId, Integer points);

    void updateStreak(UUID userId);

    void resetStreak(UUID userId);

    // ==================== MULTILINGUA ====================

    /**
     * Aggiorna la lingua preferita dell'utente
     */
    User updatePreferredLanguage(UUID userId, String language);
}