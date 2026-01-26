package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.util.Const;
import com.ai.studybuddy.model.User;
import com.ai.studybuddy.repository.UserRepository;
import com.ai.studybuddy.service.impl.UserService;
import com.ai.studybuddy.util.Const;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User save(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    public void deleteById(UUID id) {
        userRepository.deleteById(id);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("User not found with email: {}", email);
                    return new UsernameNotFoundException(Const.USER_NOT_FOUND);
                });

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                new ArrayList<>()
        );
    }

    @Override
    @Transactional
    public User registerUser(String firstName, String lastName, String email, String password) {
        if (existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, Const.EMAIL_EXISTS);
        }

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        logger.info("Registrazione nuovo utente: {}", email);
        return userRepository.save(user);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public User getCurrentUser(Principal principal) {
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, Const.UNAUTHORIZED));
    }

    @Override
    @Transactional
    public User updateProfile(Principal principal, String firstName, String lastName, String avatarUrl) {
        User user = getCurrentUser(principal);

        if (firstName != null && !firstName.isBlank()) {
            user.setFirstName(firstName);
        }
        if (lastName != null && !lastName.isBlank()) {
            user.setLastName(lastName);
        }
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }

        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void addPoints(UUID userId, Integer points) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, Const.USER_NOT_FOUND));

        user.setTotalPoints(user.getTotalPoints() + points);
        int newLevel = (user.getTotalPoints() / 100) + 1;
        user.setLevel(newLevel);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        logger.info("Utente {} ha guadagnato {} punti. Totale: {}, Livello: {}",
                userId, points, user.getTotalPoints(), user.getLevel());
    }

    @Override
    @Transactional
    public void updateStreak(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, Const.USER_NOT_FOUND));

        user.setStreakDays(user.getStreakDays() + 1);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        logger.info("Utente {} streak aggiornato a {} giorni", userId, user.getStreakDays());
    }

    @Override
    @Transactional
    public void resetStreak(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, Const.USER_NOT_FOUND));

        user.setStreakDays(0);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        logger.info("Utente {} streak resettato", userId);
    }
}