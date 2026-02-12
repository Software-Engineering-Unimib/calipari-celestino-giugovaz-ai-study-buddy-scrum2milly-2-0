

package com.ai.studybuddy.integration;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.springframework.stereotype.Component;

@Component
public class ResponseParser {

    private final Gson gson = new Gson();

    /**
     * Estrae il contenuto dalla risposta Groq (formato OpenAI)
     */
    public String extractContent(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) {
            throw new IllegalArgumentException("Risposta JSON vuota");
        }

        try {
            JsonObject response = gson.fromJson(jsonResponse, JsonObject.class);

            return response
                    .getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

        } catch (Exception e) {
            throw new RuntimeException("Errore parsing risposta Groq: " + e.getMessage(), e);
        }
    }

    /**
     * Pulisce la risposta da markdown, backticks, etc.
     */
    public String cleanResponse(String response) {
        if (response == null) {
            return "";
        }

        return response
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
    }
}