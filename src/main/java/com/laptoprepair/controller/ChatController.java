package com.laptoprepair.controller;

import com.laptoprepair.service.ChatService;
import com.laptoprepair.utils.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final RateLimiter rateLimiter;

    public ChatController(ChatService chatService, RateLimiter rateLimiter) {
        this.chatService = chatService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ChatResponse> streamChat(@RequestParam String conversation, HttpServletRequest request) {
        if (!rateLimiter.isAllowed(request)) {
            return Flux.just(createErrorResponse("Quá nhiều yêu cầu. Vui lòng thử lại sau 1 phút."));
        }

        if (conversation == null || conversation.trim().isEmpty()) {
            return Flux.just(createErrorResponse("Conversation không hợp lệ."));
        }

        return chatService.streamChatResponseWithHistory(conversation.trim())
                .onErrorResume(e -> Flux.just(createErrorResponse("Đã xảy ra lỗi khi kết nối với AI. Vui lòng thử lại sau.")));
    }
    
    private ChatResponse createErrorResponse(String message) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(message))));
    }
}