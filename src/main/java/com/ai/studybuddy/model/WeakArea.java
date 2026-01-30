package com.ai.studybuddy.model;

import com.ai.studybuddy.model.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

public class WeakArea {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User student;

    private String topic;
    private String subject;

    // Quante volte ha sbagliato su questo argomento
    private Integer errorCount = 0;

    // Ultima volta che ha sbagliato
    private LocalDateTime lastErrorAt;

    // Flag per raccomandazione prioritaria
    private Boolean needsReview = true;


}
