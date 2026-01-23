package com.ai.studybuddy.mapper;

import java.util.UUID;

import com.ai.studybuddy.dto.UserDTO;
import com.ai.studybuddy.model.User;

public class UserMapper {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private Integer level;
    private Integer totalPoints;
    private Integer streakDays;
    
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    
    public Integer getTotalPoints() { return totalPoints; }
    public void setTotalPoints(Integer totalPoints) { this.totalPoints = totalPoints; }
    
    public Integer getStreakDays() { return streakDays; }
    public void setStreakDays(Integer streakDays) { this.streakDays = streakDays; }
    
    public static UserDTO from(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setLevel(user.getLevel());
        dto.setTotalPoints(user.getTotalPoints());
        dto.setStreakDays(user.getStreakDays());
        return dto;
    }
}