package com.ai.studybuddy.controller.login;

import com.ai.studybuddy.dto.auth.LoginRequest;
import com.ai.studybuddy.dto.auth.LoginResponse;
import com.ai.studybuddy.dto.auth.RegisterRequest;
import com.ai.studybuddy.dto.auth.RegisterResponse;
import com.ai.studybuddy.model.User;
import com.ai.studybuddy.config.security.JwtUtils;
import com.ai.studybuddy.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    // ==================== REGISTRAZIONE ====================

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        logger.info("Tentativo registrazione per email: {}", request.getEmail());

        // Verifica email già esistente
        if (userService.existsByEmail(request.getEmail())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new RegisterResponse(false, "Email già registrata", null));
        }

        // Registra utente
        User user = userService.registerUser(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPassword()
        );

        logger.info("Utente registrato con successo: {}", user.getEmail());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new RegisterResponse(true, "Registrazione completata!", user.getId().toString()));
    }

    // ==================== LOGIN ====================

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        logger.info("Tentativo login per email: {}", request.getEmail());

        try {
            // Autentica utente
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // Genera token JWT
            String token = jwtUtils.generateToken(request.getEmail());

            // Recupera dati utente
            User user = userService.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("Utente non trovato"));

            logger.info("Login riuscito per: {}", request.getEmail());

            return ResponseEntity.ok(new LoginResponse(
                    true,
                    "Login riuscito!",
                    token,
                    user.getId().toString(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getEmail()
            ));

        } catch (BadCredentialsException e) {
            logger.warn("Login fallito per: {}", request.getEmail());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(false, "Email o password errati", null, null, null, null, null));
        }
    }

    // ==================== VERIFICA TOKEN ====================

    @GetMapping("/verify")
    public ResponseEntity<String> verifyToken() {
        // Se arriviamo qui, il token è valido (filtrato da JwtFilter)
        return ResponseEntity.ok("Token valido");
    }
}