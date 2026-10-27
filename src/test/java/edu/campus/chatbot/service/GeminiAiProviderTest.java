package edu.campus.chatbot.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class GeminiAiProviderTest {

    @Test
    void explainsWhenGeminiRejectsTheApiKeyWithoutPuttingItInTheUrl() {
        ChatException exception = callGeminiAndExpectError(HttpStatus.UNAUTHORIZED, "test-key");

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals(
                "Gemini could not authenticate the API key. Check that GEMINI_API_KEY contains a valid Google AI Studio key, with no extra spaces or quotes, then restart the app.",
                exception.getMessage());
    }

    @Test
    void explainsWhenGeminiDeniesKeyAccess() {
        ChatException exception = callGeminiAndExpectError(HttpStatus.FORBIDDEN, "test-key");

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals(
                "Gemini denied access. Check that the API key is allowed to use the Gemini API and that any key or API restrictions permit this app.",
                exception.getMessage());
    }

    @Test
    void explainsWhenTheConfiguredModelIsUnavailable() {
        ChatException exception = callGeminiAndExpectError(HttpStatus.NOT_FOUND, "test-key");

        assertEquals(HttpStatus.BAD_GATEWAY, exception.getStatus());
        assertEquals(
                "The configured Gemini model was not found. Check GEMINI_MODEL; the default is gemini-2.5-flash.",
                exception.getMessage());
    }

    private ChatException callGeminiAndExpectError(HttpStatus status, String apiKey) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"))
                .andExpect(header("x-goog-api-key", apiKey))
                .andRespond(withStatus(status)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"message\":\"test response\"}}"));

        GeminiAiProvider provider = new GeminiAiProvider(builder, apiKey, "gemini-2.5-flash");
        ChatException exception = assertThrows(
                ChatException.class,
                () -> provider.generate(List.of(new AiMessage("user", "Hello")), "Be helpful."));
        server.verify();
        return exception;
    }
}
