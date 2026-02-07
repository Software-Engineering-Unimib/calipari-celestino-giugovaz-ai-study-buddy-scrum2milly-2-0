package com.ai.studybuddy.util.enums;

/**
 * Livelli di difficoltà per flashcards e quiz
 */
public enum DifficultyLevel {

	PRINCIPIANTE("Principiante", 1),
	INTERMEDIO("Intermedio", 2),
	AVANZATO("Avanzato", 3);

	private final String level;
	private final int order;

	DifficultyLevel(String level, int order) {
		this.level = level;
		this.order = order;
	}

	public String getLevel() {
		return level;
	}

	public int getOrder() {
		return order;
	}

	/**
	 * Converte da stringa (case-insensitive)
	 */
	public static DifficultyLevel fromString(String value) {
		if (value == null || value.isBlank()) {
			return INTERMEDIO; // default
		}

		String normalized = value.trim().toUpperCase();

		// Match diretto con nome enum
		try {
			return DifficultyLevel.valueOf(normalized);
		} catch (IllegalArgumentException ignored) {
			// Prova con display name italiano
			for (DifficultyLevel lvl : values()) {
				if (lvl.level.equalsIgnoreCase(value.trim())) {
					return lvl;
				}
			}
		}

		return INTERMEDIO; // default fallback
	}

	/**
	 * Verifica se è più difficile di un altro livello
	 */
	public boolean isHarderThan(DifficultyLevel other) {
		return this.order > other.order;
	}

	/**
	 * Ottiene il prossimo livello di difficoltà
	 */
	public DifficultyLevel nextLevel() {
		return switch (this) {
			case PRINCIPIANTE -> INTERMEDIO;
			case INTERMEDIO -> AVANZATO;
			case AVANZATO -> AVANZATO;
		};
	}

	/**
	 * Ottiene il livello precedente di difficoltà
	 */
	public DifficultyLevel previousLevel() {
		return switch (this) {
			case PRINCIPIANTE -> PRINCIPIANTE;
			case INTERMEDIO -> PRINCIPIANTE;
			case AVANZATO -> INTERMEDIO;
		};
	}
}