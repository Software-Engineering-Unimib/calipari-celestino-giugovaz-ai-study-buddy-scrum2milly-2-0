package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.service.inter.UserService;
import com.ai.studybuddy.util.Const;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.repository.UserRepository;
import com.ai.studybuddy.util.enums.EducationLevel;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private static final String DEFAULT_LANGUAGE = "it";
    private static final List<String> SUPPORTED_LANGUAGES = Arrays.asList("it", "en", "es", "fr", "de", "pt", "ru");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ==================== CRUD BASE ====================

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

    // ==================== REGISTRAZIONE UNIFICATA ====================

    @Override
    @Transactional
    public User registerUser(String firstName, String lastName, String email, 
                            String password, EducationLevel educationLevel) {
        return registerUser(firstName, lastName, email, password, educationLevel, null);
    }

    @Override
    @Transactional
    public User registerUser(String firstName, String lastName, String email, 
                            String password, String preferredLanguage) {
        return registerUser(firstName, lastName, email, password, null, preferredLanguage);
    }

    @Override
    @Transactional
    public User registerUser(String firstName, String lastName, String email, 
                            String password, EducationLevel educationLevel, 
                            String preferredLanguage) {
        if (existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, Const.EMAIL_EXISTS);
        }

        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        
        // ⭐ IMPOSTA EDUCATION LEVEL (se presente)
        if (educationLevel != null) {
            user.setEducationLevel(educationLevel);
        }
        
        // ⭐ IMPOSTA LINGUA PREFERITA (default "it" se non specificata)
        String language = (preferredLanguage != null && !preferredLanguage.isBlank()) 
                ? preferredLanguage 
                : DEFAULT_LANGUAGE;
        
        // Valida la lingua (solo se è stata fornita esplicitamente)
        if (preferredLanguage != null && !SUPPORTED_LANGUAGES.contains(preferredLanguage)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Lingua non supportata: " + preferredLanguage);
        }
        
        user.setPreferredLanguage(language);
        
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setLevel(1);
        user.setTotalPoints(0);
        user.setStreakDays(0);

        logger.info("Registrazione nuovo utente: {} - Livello: {}, Lingua: {}", 
                email, 
                user.getEducationLevel() != null ? user.getEducationLevel().getDisplayName() : "NON SPECIFICATO",
                user.getPreferredLanguage());
                
        return userRepository.save(user);
    }

    // ==================== UTILITY ====================

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

    // ==================== GAMIFICATION ====================

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

    // ==================== MULTILINGUA ====================

    @Override
    @Transactional
    public User updatePreferredLanguage(UUID userId, String language) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, Const.USER_NOT_FOUND));

        // Valida la lingua
        if (!SUPPORTED_LANGUAGES.contains(language)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                    "Lingua non supportata. Supportate: " + SUPPORTED_LANGUAGES);
        }

        String oldLanguage = user.getPreferredLanguage();
        user.setPreferredLanguage(language);
        user.setUpdatedAt(LocalDateTime.now());
        User updated = userRepository.save(user);

        logger.info("Lingua aggiornata per utente {}: {} -> {}", 
                userId, oldLanguage != null ? oldLanguage : "it", language);
        return updated;
    }
}