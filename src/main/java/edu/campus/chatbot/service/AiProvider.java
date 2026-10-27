package edu.campus.chatbot.service;

import java.util.List;

public interface AiProvider {
    String generate(List<AiMessage> messages, String systemInstruction);
}
