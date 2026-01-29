package com.ai.studybuddy.exception;

/**
 * Eccezione per errori del servizio AI
 */
public class AIServiceException extends StudyBuddyException {

    public enum AIErrorType {
        RATE_LIMIT("Troppe richieste. Riprova tra qualche secondo."),
        INVALID_API_KEY("API Key non valida. Verifica la configurazione."),
        TIMEOUT("Il servizio AI non ha risposto in tempo. Riprova."),
        PARSE_ERROR("Errore nel parsing della risposta AI."),
        SERVICE_UNAVAILABLE("Servizio AI temporaneamente non disponibile."),
        RESPONSE_NULL("Risposta AI vuota");

        private final String defaultMessage;

        AIErrorType(String defaultMessage) {
            this.defaultMessage = defaultMessage;
        }

        public String getDefaultMessage() {
            return defaultMessage;
        }
    }

    private final AIErrorType errorType;

    public AIServiceException(AIErrorType errorType) {
        super("AI_" + errorType.name(), errorType.getDefaultMessage());
        this.errorType = errorType;
    }

    public AIServiceException(AIErrorType errorType, String customMessage) {
        super("AI_" + errorType.name(), customMessage);
        this.errorType = errorType;
    }

    public AIServiceException(AIErrorType errorType, Throwable cause) {
        super(errorType.getDefaultMessage(), cause);
        this.errorType = errorType;
    }

    public AIErrorType getErrorType() {
        return errorType;
    }
}