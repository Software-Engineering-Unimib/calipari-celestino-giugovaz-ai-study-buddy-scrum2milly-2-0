package com.ai.studybuddy.dto.auth;

import com.ai.studybuddy.util.enums.EducationLevel;

public class LoginResponse {

    private boolean success;
    private String message;
    private String token;
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
    
    // ⭐ AGGIUNTI: Campi per supportare entrambe le versioni
    private EducationLevel educationLevel;
    private String preferredLanguage;
    private Integer totalXp;
    private Integer level;
    private Integer streakDays;

    public LoginResponse() {}

    // Costruttore per versione con EducationLevel
    public LoginResponse(boolean success, String message, String token,
                         String userId, String firstName, String lastName, 
                         String email, EducationLevel educationLevel) {
        this(success, message, token, userId, firstName, lastName, email, 
             educationLevel, null, null, null, null);
    }

    // Costruttore per versione con PreferredLanguage
    public LoginResponse(boolean success, String message, String token,
                         String userId, String firstName, String lastName, 
                         String email, String preferredLanguage) {
        this(success, message, token, userId, firstName, lastName, email, 
             null, preferredLanguage, null, null, null);
    }

    // Costruttore completo (unificato)
    public LoginResponse(boolean success, String message, String token,
                         String userId, String firstName, String lastName, 
                         String email, EducationLevel educationLevel,
                         String preferredLanguage, Integer totalXp, 
                         Integer level, Integer streakDays) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.educationLevel = educationLevel;
        this.preferredLanguage = preferredLanguage;
        this.totalXp = totalXp;
        this.level = level;
        this.streakDays = streakDays;
    }

    // ==================== GETTERS & SETTERS ====================

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // ⭐ NUOVI GETTERS & SETTERS
    public EducationLevel getEducationLevel() { return educationLevel; }
    public void setEducationLevel(EducationLevel educationLevel) { 
        this.educationLevel = educationLevel; 
    }

    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { 
        this.preferredLanguage = preferredLanguage; 
    }

    public Integer getTotalXp() { return totalXp; }
    public void setTotalXp(Integer totalXp) { this.totalXp = totalXp; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public Integer getStreakDays() { return streakDays; }
    public void setStreakDays(Integer streakDays) { this.streakDays = streakDays; }

    // ⭐ Metodo builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean success;
        private String message;
        private String token;
        private String userId;
        private String firstName;
        private String lastName;
        private String email;
        private EducationLevel educationLevel;
        private String preferredLanguage;
        private Integer totalXp;
        private Integer level;
        private Integer streakDays;

        public Builder success(boolean success) { this.success = success; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder token(String token) { this.token = token; return this; }
        public Builder userId(String userId) { this.userId = userId; return this; }
        public Builder firstName(String firstName) { this.firstName = firstName; return this; }
        public Builder lastName(String lastName) { this.lastName = lastName; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder educationLevel(EducationLevel educationLevel) { 
            this.educationLevel = educationLevel; return this; 
        }
        public Builder preferredLanguage(String preferredLanguage) { 
            this.preferredLanguage = preferredLanguage; return this; 
        }
        public Builder totalXp(Integer totalXp) { this.totalXp = totalXp; return this; }
        public Builder level(Integer level) { this.level = level; return this; }
        public Builder streakDays(Integer streakDays) { this.streakDays = streakDays; return this; }

        public LoginResponse build() {
            return new LoginResponse(success, message, token, userId, firstName, 
                                     lastName, email, educationLevel, preferredLanguage, 
                                     totalXp, level, streakDays);
        }
    }
}