package edu.campus.chatbot.api;

import edu.campus.chatbot.api.dto.ChatRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "gemini.api-key=")
class ChatControllerIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void servesTheChatInterface() {
        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("<title>Campus AI Chatbot</title>"));
        assertTrue(response.getBody().contains("src=\"/app.js\""));
    }

    @Test
    void reportsMissingGeminiKeyAsAFriendlyServiceError() {
        ChatRequest request = new ChatRequest("Hello", List.of(), "general");

        ResponseEntity<ChatExceptionHandler.ApiError> response =
                restTemplate.postForEntity("/api/chat", request, ChatExceptionHandler.ApiError.class);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().error().contains("GEMINI_API_KEY"));
    }

    @Test
    void rejectsAnEmptyMessage() {
        ChatRequest request = new ChatRequest(" ", List.of(), "general");

        ResponseEntity<ChatExceptionHandler.ApiError> response =
                restTemplate.postForEntity("/api/chat", request, ChatExceptionHandler.ApiError.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().error().contains("enter a message"));
    }
}
