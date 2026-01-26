package com.ai.studybuddy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())  // Disabilita CSRF per test
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**").permitAll()  // Permetti tutte le API
                .requestMatchers("/test/**").permitAll()  // Permetti test endpoints
                .requestMatchers("/h2-console/**").permitAll()  // Permetti H2
                .requestMatchers("/**").permitAll()  // Permetti tutto il resto
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.disable())  // Per H2 console
            );

        return http.build();
    }
}
