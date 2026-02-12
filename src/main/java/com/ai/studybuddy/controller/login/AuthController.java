package com.ai.studybuddy.controller.login;

import com.ai.studybuddy.util.Const;
import com.ai.studybuddy.dto.auth.LoginRequest;
import com.ai.studybuddy.dto.auth.LoginResponse;
import com.ai.studybuddy.dto.auth.RegisterRequest;
import com.ai.studybuddy.dto.auth.RegisterResponse;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.config.security.JwtUtils;
import com.ai.studybuddy.service.inter.UserService;
import com.ai.studybuddy.util.enums.EducationLevel;
import jakarta.validation.Valid;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public AuthController(UserService userService, 
                         AuthenticationManager authenticationManager, 
                         JwtUtils jwtUtils) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }

    // ==================== REGISTRAZIONE ====================

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        logger.info("Tentativo registrazione per email: {} - Livello: {} - Lingua: {}", 
                request.getEmail(),
                request.getEducationLevel() != null ? request.getEducationLevel().getDisplayName() : "NON SPECIFICATO",
                request.getPreferredLanguage() != null ? request.getPreferredLanguage() : "it");

        if (userService.existsByEmail(request.getEmail())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new RegisterResponse(false, Const.EMAIL_EXISTS, null));
        }

        // REGISTRAZIONE UNIFICATA CON ENTRAMBI I PARAMETRI
        User user = userService.registerUser(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPassword(),
                request.getEducationLevel() != null ? request.getEducationLevel() : EducationLevel.UNIVERSITY,
                request.getPreferredLanguage() != null ? request.getPreferredLanguage() : "it"
        );

        logger.info("Utente registrato con successo: {} - Livello: {} - Lingua: {}", 
                user.getEmail(), 
                user.getEducationLevel().getDisplayName(),
                user.getPreferredLanguage());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new RegisterResponse(true, Const.REGISTRATION_SUCCESS, user.getId().toString()));
    }

    // ==================== LOGIN ====================

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("Tentativo login per email: {}", request.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            String token = jwtUtils.generateToken(request.getEmail());

            User user = userService.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BadCredentialsException(Const.USER_NOT_FOUND));

            logger.info("Login riuscito per: {} (lingua: {}, livello: {})", 
                    request.getEmail(), 
                    user.getPreferredLanguage(),
                    user.getEducationLevel().getDisplayName());

            // ✅ COSTRUTTORE COMPLETO - 11 parametri
            return ResponseEntity.ok(new LoginResponse(
                    true,
                    Const.LOGIN_SUCCESS,
                    token,
                    user.getId().toString(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getEducationLevel(),
                    user.getPreferredLanguage(),
                    user.getTotalPoints(),
                    user.getLevel(),
                    user.getStreakDays()
            ));

        } catch (BadCredentialsException e) {
            logger.warn("Login fallito per: {}", request.getEmail());
            
            // ✅ COSTRUTTORE COMPLETO - 11 parametri con valori null/default
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(
                            false,
                            Const.INVALID_CREDENTIALS,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    ));
        }
    }

    // ==================== GESTIONE LINGUA ====================

    @PutMapping("/update-language")
    public ResponseEntity<Map<String, Object>> updateLanguage(
            @Valid @RequestBody Map<String, String> request,
            Principal principal) {

        User user = userService.getCurrentUser(principal);
        String language = request.get("preferredLanguage");

        if (language == null || language.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("success", false, "message", "Lingua non specificata"));
        }

        try {
            User updated = userService.updatePreferredLanguage(user.getId(), language);
            
            logger.info("Lingua aggiornata per {}: {} -> {}", 
                    user.getEmail(), user.getPreferredLanguage(), language);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Lingua aggiornata con successo",
                    "preferredLanguage", updated.getPreferredLanguage()
            ));

        } catch (Exception e) {
            logger.error("Errore aggiornamento lingua: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ==================== INFO UTENTE ====================

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUserInfo(Principal principal) {
        User user = userService.getCurrentUser(principal);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("firstName", user.getFirstName());
        userInfo.put("lastName", user.getLastName());
        userInfo.put("email", user.getEmail());
        userInfo.put("preferredLanguage", user.getPreferredLanguage());
        userInfo.put("educationLevel", user.getEducationLevel());
        userInfo.put("level", user.getLevel());
        userInfo.put("totalPoints", user.getTotalPoints());
        userInfo.put("streakDays", user.getStreakDays());
        userInfo.put("createdAt", user.getCreatedAt());

        return ResponseEntity.ok(userInfo);
    }

    // ==================== VERIFICA TOKEN ====================

    @GetMapping("/verify")
    public ResponseEntity<String> verifyToken() {
        return ResponseEntity.ok(Const.TOKEN_VALID);
    }

}