package com.ai.studybuddy.exception;

/**
 * Eccezione base per AI Study Buddy
 */
public class StudyBuddyException extends RuntimeException {

    private final String errorCode;

    public StudyBuddyException(String message) {
        super(message);
        this.errorCode = "GENERIC_ERROR";
    }

    public StudyBuddyException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public StudyBuddyException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "GENERIC_ERROR";
    }

    public String getErrorCode() {
        return errorCode;
    }
}