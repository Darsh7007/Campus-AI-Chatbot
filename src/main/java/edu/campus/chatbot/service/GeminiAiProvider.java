package edu.campus.chatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Component
public class GeminiAiProvider implements AiProvider {
    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiAiProvider(
            RestClient.Builder restClientBuilder,
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-2.5-flash}") String model) {
        this.restClient = restClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String generate(List<AiMessage> messages, String systemInstruction) {
        if (apiKey.isBlank()) {
            throw new ChatException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "The chatbot is not configured yet. Set GEMINI_API_KEY in the terminal, then restart the app.");
        }

        List<Map<String, Object>> contents = messages.stream()
                .map(message -> Map.<String, Object>of(
                        "role", message.role(),
                        "parts", List.of(Map.of("text", message.content()))))
                .toList();

        Map<String, Object> requestBody = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
                "contents", contents);

        try {
            JsonNode response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .build(model))
                    .header("x-goog-api-key", apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            String answer = extractAnswer(response);
            if (answer.isBlank()) {
                throw new ChatException(
                        HttpStatus.BAD_GATEWAY,
                        "The AI could not produce a reply for that message. Please try rephrasing it.");
            }
            return answer;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 429) {
                throw new ChatException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "The AI service is busy right now. Please wait a moment and try again.");
            }
            if (exception.getStatusCode().value() == 401) {
                throw new ChatException(
                        HttpStatus.BAD_GATEWAY,
                        "Gemini could not authenticate the API key. Check that GEMINI_API_KEY contains a valid Google AI Studio key, with no extra spaces or quotes, then restart the app.");
            }
            if (exception.getStatusCode().value() == 403) {
                throw new ChatException(
                        HttpStatus.BAD_GATEWAY,
                        "Gemini denied access. Check that the API key is allowed to use the Gemini API and that any key or API restrictions permit this app.");
            }
            if (exception.getStatusCode().value() == 404) {
                throw new ChatException(
                        HttpStatus.BAD_GATEWAY,
                        "The configured Gemini model was not found. Check GEMINI_MODEL; the default is gemini-2.5-flash.");
            }
            if (exception.getStatusCode().is4xxClientError()) {
                throw new ChatException(
                        HttpStatus.BAD_GATEWAY,
                        "Gemini rejected the request. Check the configured model and try a shorter message.");
            }
            throw new ChatException(
                    HttpStatus.BAD_GATEWAY,
                    "The AI service is temporarily unavailable. Please try again shortly.");
        } catch (ResourceAccessException exception) {
            throw new ChatException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Could not reach Gemini. Check your internet connection and try again.");
        }
    }

    private String extractAnswer(JsonNode response) {
        if (response == null || !response.path("candidates").isArray()
                || response.path("candidates").isEmpty()) {
            return "";
        }

        StringBuilder answer = new StringBuilder();
        for (JsonNode part : response.path("candidates").path(0).path("content").path("parts")) {
            if (part.hasNonNull("text")) {
                if (!answer.isEmpty()) {
                    answer.append('\n');
                }
                answer.append(part.path("text").asText());
            }
        }
        return answer.toString().trim();
    }
}
