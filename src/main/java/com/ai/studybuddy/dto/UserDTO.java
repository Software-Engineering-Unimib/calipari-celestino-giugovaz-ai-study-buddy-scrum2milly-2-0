package com.ai.studybuddy.dto;

import com.ai.studybuddy.util.enums.EducationLevel;
import java.time.LocalDateTime;
import java.util.UUID;

public class UserDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private EducationLevel educationLevel;
    private String preferredLanguage;
    private String avatarUrl;
    private Integer level;
    private Integer totalPoints;
    private Integer streakDays;
    private LocalDateTime createdAt;
    
    // Getter
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public UUID getId() {
        return id;
    }
    
    public String getFirstName() {
        return firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public EducationLevel getEducationLevel() {
        return educationLevel;
    }
    
    public String getPreferredLanguage() {
        return preferredLanguage;
    }
    
    public String getAvatarUrl() {
        return avatarUrl;
    }
    
    public Integer getLevel() {
        return level;
    }
    
    public Integer getTotalPoints() {
        return totalPoints;
    }
    
    public Integer getStreakDays() {
        return streakDays;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    // Setter
    public void setId(UUID id) {
        this.id = id;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public void setEducationLevel(EducationLevel educationLevel) {
        this.educationLevel = educationLevel;
    }
    
    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }
    
    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
    
    public void setLevel(Integer level) {
        this.level = level;
    }
    
    public void setTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints;
    }
    
    public void setStreakDays(Integer streakDays) {
        this.streakDays = streakDays;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}