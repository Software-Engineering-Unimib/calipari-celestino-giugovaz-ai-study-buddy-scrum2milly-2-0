package com.ai.studybuddy.dto.auth;

import com.ai.studybuddy.util.enums.EducationLevel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @Size(min = 2, max = 50, message = "Il nome deve essere tra 2 e 50 caratteri")
    private String firstName;

    @Size(min = 2, max = 50, message = "Il cognome deve essere tra 2 e 50 caratteri")
    private String lastName;

    @Email(message = "Formato email non valido")
    @Size(max = 100, message = "L'email non può superare 100 caratteri")
    private String email;

    @Size(min = 8, message = "La password deve essere di almeno 8 caratteri")
    private String password;

    // ⭐ SUPPORTA ENTRAMBI I PARADIGMI
    private EducationLevel educationLevel;
    
    @Size(min = 2, max = 10, message = "La lingua deve essere tra 2 e 10 caratteri")
    private String preferredLanguage = "it";

    public RegisterRequest() {}

    // Costruttore per versione con EducationLevel
    public RegisterRequest(String firstName, String lastName, String email, 
                          String password, EducationLevel educationLevel) {
        this(firstName, lastName, email, password, educationLevel, "it");
    }

    // Costruttore per versione con PreferredLanguage
    public RegisterRequest(String firstName, String lastName, String email, 
                          String password, String preferredLanguage) {
        this(firstName, lastName, email, password, null, preferredLanguage);
    }

    // Costruttore completo (unificato)
    public RegisterRequest(String firstName, String lastName, String email, 
                          String password, EducationLevel educationLevel, 
                          String preferredLanguage) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.educationLevel = educationLevel;
        this.preferredLanguage = preferredLanguage != null ? preferredLanguage : "it";
    }

    // ==================== GETTERS & SETTERS ====================

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public EducationLevel getEducationLevel() { return educationLevel; }
    public void setEducationLevel(EducationLevel educationLevel) { 
        this.educationLevel = educationLevel; 
    }

    public String getPreferredLanguage() { 
        return preferredLanguage != null ? preferredLanguage : "it"; 
    }
    public void setPreferredLanguage(String preferredLanguage) { 
        this.preferredLanguage = preferredLanguage; 
    }

    // ⭐ Metodi utility
    public boolean hasEducationLevel() {
        return educationLevel != null;
    }

    public boolean hasPreferredLanguage() {
        return preferredLanguage != null && !preferredLanguage.isEmpty();
    }

    // ⭐ Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String firstName;
        private String lastName;
        private String email;
        private String password;
        private EducationLevel educationLevel;
        private String preferredLanguage = "it";

        public Builder firstName(String firstName) { this.firstName = firstName; return this; }
        public Builder lastName(String lastName) { this.lastName = lastName; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder password(String password) { this.password = password; return this; }
        public Builder educationLevel(EducationLevel educationLevel) { 
            this.educationLevel = educationLevel; return this; 
        }
        public Builder preferredLanguage(String preferredLanguage) { 
            this.preferredLanguage = preferredLanguage; return this; 
        }

        public RegisterRequest build() {
            return new RegisterRequest(firstName, lastName, email, password, 
                                       educationLevel, preferredLanguage);
        }
    }
}