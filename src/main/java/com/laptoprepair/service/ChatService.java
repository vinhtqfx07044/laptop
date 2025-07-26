package com.laptoprepair.service;

import reactor.core.publisher.Flux;
import org.springframework.ai.chat.model.ChatResponse;

public interface ChatService {
    
    /**
     * Stream chat response with conversation history using Spring AI native streaming
     */
    Flux<ChatResponse> streamChatResponseWithHistory(String conversationJson);
}