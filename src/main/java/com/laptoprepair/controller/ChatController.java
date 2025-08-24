package com.laptoprepair.controller;

import com.laptoprepair.utils.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for handling chat interactions with an AI model.
 * Provides an endpoint for streaming chat responses with rate limiting and
 * conversation memory.
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private final ChatClient chatClient;
    private final RateLimiter rateLimiter;
    private final ChatMemory chatMemory;

    @Value("${app.chat.max-user-messages}")
    private int maxUserMessages;

    public ChatController(ChatClient chatClient, RateLimiter rateLimiter, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.rateLimiter = rateLimiter;
        this.chatMemory = chatMemory;
    }

    /**
     * Streams chat responses from the AI model.
     * Applies rate limiting and manages conversation history.
     * 
     * @param message        The user's message.
     * @param conversationId Optional. The ID of the ongoing conversation. If null,
     *                       a new conversation is started.
     * @param request        The HttpServletRequest for rate limiting.
     * @return A Flux of ChatResponse objects, representing the streamed AI
     *         response.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> streamChat(
            @RequestParam String message,
            @RequestParam(required = false) String conversationId,
            HttpServletRequest request) {

        if (!rateLimiter.isAllowed(request, "chat")) {
            return Flux.just(createErrorResponse("Quá nhiều yêu cầu. Vui lòng thử lại sau 1 phút."));
        }

        if (message == null || message.trim().isEmpty()) {
            return Flux.just(createErrorResponse("Vui lòng nhập tin nhắn."));
        }

        String sessionId = conversationId != null && !conversationId.trim().isEmpty()
                ? conversationId.trim()
                : UUID.randomUUID().toString();

        if (conversationId != null && !conversationId.trim().isEmpty()) {
            List<Message> existingMessages = chatMemory.get(sessionId);
            long userMessageCount = existingMessages.stream()
                    .filter(msg -> msg.getMessageType() == MessageType.USER)
                    .count();

            if (userMessageCount >= maxUserMessages) {
                return Flux.just(createErrorResponse("Đã đạt giới hạn " + maxUserMessages
                        + " tin nhắn. Vui lòng nhấn nút làm mới cuộc trò chuyện."));
            }
        }

        return chatClient.prompt()
                .user(message.trim())
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream()
                .chatResponse()
                .onErrorResume(e -> {
                    logger.error("Chat streaming error: {}", e.getMessage(), e);
                    return Flux.just(createErrorResponse("Đã xảy ra lỗi khi kết nối với AI. Vui lòng thử lại sau."));
                });
    }

    private ChatResponse createErrorResponse(String message) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(message))));
    }
}