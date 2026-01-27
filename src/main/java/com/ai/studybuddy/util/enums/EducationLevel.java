package com.ai.studybuddy.util.enums;

public enum EducationLevel {
    MIDDLE_SCHOOL("Middle School"),
    HIGH_SCHOOL("High School"),
    UNIVERSITY("University"),
    POST_GRADUATE("Post Graduate"),
    OTHER("Other");

    private final String displayName;

    EducationLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
