package edu.campus.chatbot.api.dto;

import java.util.List;

public record ChatRequest(String message, List<ChatMessage> history, String mode) {
}
