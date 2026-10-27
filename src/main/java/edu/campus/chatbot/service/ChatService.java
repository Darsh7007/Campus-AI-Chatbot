package edu.campus.chatbot.service;

import edu.campus.chatbot.api.dto.ChatMessage;
import edu.campus.chatbot.api.dto.ChatRequest;
import edu.campus.chatbot.api.dto.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {
    private static final int MAX_MESSAGE_LENGTH = 4_000;
    private static final int MAX_HISTORY_MESSAGES = 20;
    private static final String COLLEGE_INSTRUCTION =
            "You are in college-assistant mode. Help with general college questions, " +
            "but clearly say when you do not know private or institution-specific information. " +
            "Do not invent college policies, schedules, contacts, or services. Use details only " +
            "when the user provides them.";

    private final AiProvider aiProvider;
    private final String systemInstruction;

    public ChatService(
            AiProvider aiProvider,
            @Value("${chatbot.system-instruction}") String systemInstruction) {
        this.aiProvider = aiProvider;
        this.systemInstruction = systemInstruction;
    }

    public ChatResponse chat(ChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            throw new ChatException(HttpStatus.BAD_REQUEST, "Please enter a message before sending.");
        }
        if (request.message().length() > MAX_MESSAGE_LENGTH) {
            throw new ChatException(HttpStatus.BAD_REQUEST, "Messages must be 4,000 characters or fewer.");
        }

        List<ChatMessage> history = request.history() == null ? List.of() : request.history();
        if (history.size() > MAX_HISTORY_MESSAGES) {
            throw new ChatException(HttpStatus.BAD_REQUEST, "Please start a new chat to continue.");
        }

        List<AiMessage> messages = new ArrayList<>();
        for (ChatMessage item : history) {
            if (item == null || item.content() == null || item.content().isBlank()
                    || item.content().length() > MAX_MESSAGE_LENGTH
                    || (!"user".equals(item.role()) && !"assistant".equals(item.role()))) {
                throw new ChatException(HttpStatus.BAD_REQUEST, "The chat history was not valid. Please start a new chat.");
            }
            String role = "assistant".equals(item.role()) ? "model" : "user";
            messages.add(new AiMessage(role, item.content()));
        }

        String mode = request.mode() == null ? "general" : request.mode();
        if (!"general".equals(mode) && !"college".equals(mode)) {
            throw new ChatException(HttpStatus.BAD_REQUEST, "Choose a valid chat mode.");
        }

        messages.add(new AiMessage("user", request.message().trim()));
        String instruction = "college".equals(mode)
                ? systemInstruction + "\n\n" + COLLEGE_INSTRUCTION
                : systemInstruction;
        return new ChatResponse(aiProvider.generate(messages, instruction));
    }
}
