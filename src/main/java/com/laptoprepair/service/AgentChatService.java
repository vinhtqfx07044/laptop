package com.laptoprepair.service;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;

/**
 * Agent-based chat service with tool calling capabilities.
 * Uses Spring AI's ChatClient with tool callbacks and memory advisors.
 * The agent can make multiple tool calls sequentially during processing.
 */
public interface AgentChatService {

    Flux<ChatResponse> processMessage(
            String conversationId,
            String userMessage,
            Authentication authentication);
}
