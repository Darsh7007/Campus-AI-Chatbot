package edu.campus.chatbot.service;

import edu.campus.chatbot.api.dto.ChatMessage;
import edu.campus.chatbot.api.dto.ChatRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatServiceTest {

    @Test
    void sendsCurrentPromptAndConversationHistoryToProvider() {
        AtomicReference<List<AiMessage>> sentMessages = new AtomicReference<>();
        AiProvider provider = (messages, instruction) -> {
            sentMessages.set(messages);
            return "A helpful answer";
        };
        ChatService service = new ChatService(provider, "Be helpful.");

        service.chat(new ChatRequest(
                " What is a variable? ",
                List.of(new ChatMessage("user", "Hello"), new ChatMessage("assistant", "Hi")),
                "general"));

        assertEquals(List.of(
                new AiMessage("user", "Hello"),
                new AiMessage("model", "Hi"),
                new AiMessage("user", "What is a variable?")), sentMessages.get());
    }

    @Test
    void rejectsEmptyMessagesBeforeCallingProvider() {
        ChatService service = new ChatService((messages, instruction) -> "unused", "Be helpful.");

        ChatException exception = assertThrows(
                ChatException.class,
                () -> service.chat(new ChatRequest("  ", List.of(), "general")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
}
