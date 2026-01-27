package com.ai.studybuddy.exception;

/**
 * Eccezione per accesso non autorizzato
 */
public class UnauthorizedException extends StudyBuddyException {

    public UnauthorizedException() {
        super("UNAUTHORIZED", "Non autorizzato ad accedere a questa risorsa");
    }

    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }

    public UnauthorizedException(String resourceName, String action) {
        super("UNAUTHORIZED",
                String.format("Non autorizzato a %s %s", action, resourceName));
    }
}