package com.ai.studybuddy.service.inter;

import com.ai.studybuddy.model.user.User;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserService {

    List<User> getAllUsers();

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    User save(User user);

    void deleteById(UUID id);

    /**
     * Registra un nuovo utente con supporto multilingua
     * 
     * @param firstName nome
     * @param lastName cognome
     * @param email email
     * @param password password
     * @param preferredLanguage lingua preferita (it, en, es, fr, de, pt, ru)
     * @return utente registrato
     */
    User registerUser(String firstName, String lastName, String email, String password, String preferredLanguage);

    boolean existsByEmail(String email);

    User getCurrentUser(Principal principal);

    User updateProfile(Principal principal, String firstName, String lastName, String avatarUrl);

    void addPoints(UUID userId, Integer points);

    void updateStreak(UUID userId);

    void resetStreak(UUID userId);

    User updatePreferredLanguage(UUID userId, String language);
}