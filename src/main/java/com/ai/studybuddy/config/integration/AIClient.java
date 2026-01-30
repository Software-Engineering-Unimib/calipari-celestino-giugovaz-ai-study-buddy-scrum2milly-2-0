package com.ai.studybuddy.config.integration;

/**
 * Interfaccia per client di servizi AI.
 *
 * Implementa il Strategy Pattern permettendo l'utilizzo intercambiabile
 * di diversi modelli AI (es. Llama 3.3 70B vs Mixtral 8x7B).
 *
 *
 */

public interface AIClient {

    /**
     * Genera testo usando il modello AI.
     *
     * @param prompt il prompt da inviare al modello
     * @return il testo generato dal modello
     * @throws RuntimeException se la chiamata fallisce
     */
    String generateText(String prompt);

    /**
     * Verifica se il client AI è disponibile e funzionante.
     *
     * @return true se il servizio è disponibile, false altrimenti
     */
    boolean isAvailable();

    /**
     * Restituisce il nome del modello utilizzato.
     *
     * @return il nome del modello (es. "llama-3.3-70b-versatile (Primary)")
     */
    String getModelName();
}
