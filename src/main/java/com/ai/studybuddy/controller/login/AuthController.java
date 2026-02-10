package com.ai.studybuddy.controller.login;

import com.ai.studybuddy.util.Const;
import com.ai.studybuddy.dto.auth.LoginRequest;
import com.ai.studybuddy.dto.auth.LoginResponse;
import com.ai.studybuddy.dto.auth.RegisterRequest;
import com.ai.studybuddy.dto.auth.RegisterResponse;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.config.security.JwtUtils;
import com.ai.studybuddy.service.inter.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    // Constructor injection
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
        logger.info("Richiesta registrazione per: {} - Livello: {}",
                request.getEmail(),
                request.getEducationLevel().getDisplayName());

        User user = userService.registerUser(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPassword(),
                request.getEducationLevel()
        );

        RegisterResponse response = new RegisterResponse(
                true,
                "Registrazione completata con successo!",
                user.getId().toString()

        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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

            logger.info("Login riuscito per: {}", request.getEmail());

            return ResponseEntity.ok(new LoginResponse(
                    true,
                    Const.LOGIN_SUCCESS,
                    token,
                    user.getId().toString(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail(),
                    user.getEducationLevel() != null ? user.getEducationLevel() : null
            ));

        } catch (BadCredentialsException e) {
            logger.warn("Login fallito per: {}", request.getEmail());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(false, Const.INVALID_CREDENTIALS, null, null, null, null, null, null));
        }
    }

    // ==================== VERIFICA TOKEN ====================

    @GetMapping("/verify")
    public ResponseEntity<String> verifyToken() {
        return ResponseEntity.ok(Const.TOKEN_VALID);
    }
}