package com.ai.studybuddy.dto.auth;

import com.ai.studybuddy.util.enums.EducationLevel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @Size(min = 2, max = 50)
    private String firstName;

    @Size(min = 2, max = 50)
    private String lastName;

    @Email()
    private String email;

    @Size(min = 8)
    private String password;


    private EducationLevel educationLevel;

    public RegisterRequest() {}


    public RegisterRequest(String firstName, String lastName, String email, String password, EducationLevel educationLevel) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.educationLevel = educationLevel;
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

    public EducationLevel getEducationLevel(){

        return educationLevel;
    }

    public void setEducationLevel(EducationLevel educationLevel) {
        this.educationLevel = educationLevel;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}