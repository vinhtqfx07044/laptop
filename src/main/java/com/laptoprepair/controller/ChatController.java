package com.laptoprepair.controller;

import com.laptoprepair.utils.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatClient chatClient;
    private final RateLimiter rateLimiter;

    public ChatController(ChatClient chatClient, RateLimiter rateLimiter) {
        this.chatClient = chatClient;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> streamChat(
            @RequestParam String message,
            @RequestParam(required = false) String conversationId,
            HttpServletRequest request) {
        
        if (!rateLimiter.isAllowed(request)) {
            return Flux.just(createErrorResponse("Quá nhiều yêu cầu. Vui lòng thử lại sau 1 phút."));
        }

        if (message == null || message.trim().isEmpty()) {
            return Flux.just(createErrorResponse("Vui lòng nhập tin nhắn."));
        }

        // Use provided conversation ID or generate new one
        String sessionId = conversationId != null && !conversationId.trim().isEmpty() 
                          ? conversationId.trim() 
                          : UUID.randomUUID().toString();

        return chatClient.prompt()
                .user(message.trim())
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream()
                .chatResponse()
                .onErrorResume(e -> {
                    System.err.println("Chat streaming error: " + e.getMessage());
                    return Flux.just(createErrorResponse("Đã xảy ra lỗi khi kết nối với AI. Vui lòng thử lại sau."));
                });
    }

    private ChatResponse createErrorResponse(String message) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(message))));
    }
}