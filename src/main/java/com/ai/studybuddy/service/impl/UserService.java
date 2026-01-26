package com.ai.studybuddy.service;

import com.ai.studybuddy.model.User;

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

    User registerUser(String firstName, String lastName, String email, String password);

    boolean existsByEmail(String email);

    User getCurrentUser(Principal principal);

    User updateProfile(Principal principal, String firstName, String lastName, String avatarUrl);

    void addPoints(UUID userId, Integer points);

    void updateStreak(UUID userId);

    void resetStreak(UUID userId);
}