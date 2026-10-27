package edu.campus.chatbot.api;

import edu.campus.chatbot.service.ChatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ChatExceptionHandler {

    @ExceptionHandler(ChatException.class)
    public ResponseEntity<ApiError> handleChatException(ChatException exception) {
        return ResponseEntity.status(exception.getStatus()).body(new ApiError(exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleUnreadableRequest() {
        return new ApiError("That chat request was not valid. Please try again.");
    }

    public record ApiError(String error) {
    }
}
