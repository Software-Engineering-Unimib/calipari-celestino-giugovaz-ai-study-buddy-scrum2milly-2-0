package com.ai.studybuddy.util.enums;

public enum EducationLevel {
    MIDDLE_SCHOOL("Scuola Media"),
    HIGH_SCHOOL("Scuola Superiore"),
    UNIVERSITY("Università"),
    ALTRO("Altro");

    private final String displayName;

    EducationLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Converte una stringa nel corrispondente EducationLevel
     * @param value stringa da convertire (accetta sia il nome enum che il displayName)
     * @return EducationLevel corrispondente
     * @throws IllegalArgumentException se il valore non è valido
     */
    public static EducationLevel fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Il livello di educazione non può essere vuoto");
        }

        for (EducationLevel level : values()) {
            if (level.name().equalsIgnoreCase(value) ||
                    level.displayName.equalsIgnoreCase(value)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Livello di educazione non valido: " + value);
    }
}
