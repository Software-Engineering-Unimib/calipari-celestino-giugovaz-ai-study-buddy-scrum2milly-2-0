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
    private EducationLevel educationLevel;

    public LoginResponse() {}

    public LoginResponse(boolean success, String message, String token,
                         String userId, String firstName, String lastName, String email, EducationLevel educationLevel) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.educationLevel = educationLevel;
    }

    // Getters e Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}