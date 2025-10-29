package com.laptoprepair.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.laptoprepair.interceptor.RateLimiter;
import com.laptoprepair.service.AgentChatService;

import reactor.core.publisher.Flux;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for handling chat interactions with an AI agent.
 * Provides an endpoint for streaming chat responses with rate limiting,
 * conversation memory, and role-based tool access.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private final AgentChatService agentChatService;
    private final RateLimiter rateLimiter;
    private final ChatMemory chatMemory;

    @Value("${laptoprepair.ai.chat.max-user-messages}")
    private int maxUserMessages;

    public ChatController(AgentChatService agentChatService, RateLimiter rateLimiter, ChatMemory chatMemory) {
        this.agentChatService = agentChatService;
        this.rateLimiter = rateLimiter;
        this.chatMemory = chatMemory;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> streamChat(
            @RequestParam @NonNull String message,
            @RequestParam(required = false) @Nullable String conversationId,
            HttpServletRequest request) {

        if (!rateLimiter.isAllowed(request, "chat")) {
            return Flux.just(new ChatResponse(
                    List.of(new Generation(new AssistantMessage("Quá nhiều yêu cầu. Vui lòng thử lại sau 1 phút.")))));
        }

        if (message.trim().isEmpty()) {
            return Flux
                    .just(new ChatResponse(List.of(new Generation(new AssistantMessage("Vui lòng nhập tin nhắn.")))));
        }

        String sessionId = generateSessionId(conversationId);

        Flux<ChatResponse> messageLimitCheck = checkMessageLimit(conversationId, sessionId);
        if (messageLimitCheck != null) {
            return messageLimitCheck;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return agentChatService.processMessage(sessionId, message.trim(), authentication)
                .onErrorResume(e -> {
                    logger.error("Agent error: {}", e.getMessage(), e);
                    return Flux.just(new ChatResponse(List.of(new Generation(
                            new AssistantMessage("Đã xảy ra lỗi khi kết nối với AI. Vui lòng thử lại sau.")))));
                });
    }

    private @NonNull String generateSessionId(@Nullable String conversationId) {
        if (conversationId != null && !conversationId.trim().isEmpty()) {
            String trimmedId = conversationId.trim();
            if (trimmedId != null) {
                return trimmedId;
            }
        }
        String uuid = UUID.randomUUID().toString();
        return uuid != null ? uuid : "fallback-" + System.currentTimeMillis();
    }

    private Flux<ChatResponse> checkMessageLimit(@Nullable String conversationId, @NonNull String sessionId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return null;
        }

        List<Message> existingMessages = chatMemory.get(sessionId);

        long userMessageCount = existingMessages.stream()
                .filter(msg -> msg != null && msg.getMessageType() == MessageType.USER)
                .count();

        if (userMessageCount >= maxUserMessages) {
            String errorMessage = "Đã đạt giới hạn " + maxUserMessages
                    + " tin nhắn. Vui lòng nhấn nút làm mới cuộc trò chuyện.";
            return Flux.just(new ChatResponse(List.of(new Generation(new AssistantMessage(errorMessage)))));
        }

        return null;
    }
}