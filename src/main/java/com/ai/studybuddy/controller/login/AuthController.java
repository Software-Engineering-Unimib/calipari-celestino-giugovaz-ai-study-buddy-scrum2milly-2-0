package com.ai.studybuddy.controller.login;

import com.ai.studybuddy.util.Const;
import com.ai.studybuddy.dto.auth.LoginRequest;
import com.ai.studybuddy.dto.auth.LoginResponse;
import com.ai.studybuddy.dto.auth.RegisterRequest;
import com.ai.studybuddy.dto.auth.RegisterResponse;
import com.ai.studybuddy.model.User;
import com.ai.studybuddy.config.security.JwtUtils;
import com.ai.studybuddy.service.impl.UserService;
import com.ai.studybuddy.util.Const;
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

        if (userService.existsByEmail(request.getEmail())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(new RegisterResponse(false, Const.EMAIL_EXISTS, null));
        }

        User user = userService.registerUser(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPassword()
        );

        logger.info("Utente registrato con successo: {}", user.getEmail());

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

            logger.info("Login riuscito per: {}", request.getEmail());

            return ResponseEntity.ok(new LoginResponse(
                    true,
                    Const.LOGIN_SUCCESS,
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
                    .body(new LoginResponse(false, Const.INVALID_CREDENTIALS, null, null, null, null, null));
        }
    }

    // ==================== VERIFICA TOKEN ====================

    @GetMapping("/verify")
    public ResponseEntity<String> verifyToken() {
        return ResponseEntity.ok(Const.TOKEN_VALID);
    }
}