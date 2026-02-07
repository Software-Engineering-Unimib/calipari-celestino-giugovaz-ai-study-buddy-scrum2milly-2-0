package com.ai.studybuddy.exception;

/**
 * Eccezione per accesso non autorizzato
 */
public class UnauthorizedException extends StudyBuddyException {

    private static final String ERROR_CODE = "UNAUTHORIZED";

    public UnauthorizedException() {
        super(ERROR_CODE, "Non autorizzato ad accedere a questa risorsa");
    }

    public UnauthorizedException(String message) {
        super(ERROR_CODE, message);
    }

    public UnauthorizedException(String resourceName, String action) {
        super(ERROR_CODE,
                String.format("Non autorizzato a %s %s", action, resourceName));
    }
}