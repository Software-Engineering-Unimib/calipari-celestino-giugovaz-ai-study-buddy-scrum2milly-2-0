package com.ai.studybuddy.exception;

/**
 * Eccezione per risorse non trovate
 */
public class ResourceNotFoundException extends StudyBuddyException {

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super("NOT_FOUND",
                String.format("%s non trovato con %s: '%s'", resourceName, fieldName, fieldValue));
    }

    public ResourceNotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}