package com.ai.studybuddy.util.enums;

public enum DifficultyLevel {

    PRINCIPIANTE("Principiante"),
    INTERMEDIO("Intermedio"),
    AVANZATO("Avanzanto");
    private final String level;
    DifficultyLevel(String level){

        this.level = level;

    }

    public String getLevel(){

        return level;

    }

}
