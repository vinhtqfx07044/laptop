package com.laptoprepair.service.impl;

import com.laptoprepair.service.ChatService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.ArrayList;

@Service
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public ChatServiceImpl(ChatClient chatClient, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Flux<ChatResponse> streamChatResponseWithHistory(String conversationJson) {
        try {
            // Parse conversation history
            JsonNode conversationNode = objectMapper.readTree(conversationJson);
            List<Message> messages = new ArrayList<>();
            
            for (JsonNode messageNode : conversationNode) {
                String role = messageNode.get("role").asText();
                String content = messageNode.get("content").asText();
                
                if ("user".equals(role)) {
                    messages.add(new UserMessage(content));
                } else if ("assistant".equals(role)) {
                    messages.add(new AssistantMessage(content));
                }
            }
            
            // Use ChatClient with conversation history
            return chatClient.prompt()
                    .messages(messages)
                    .stream()
                    .chatResponse();
                    
        } catch (Exception e) {
            System.err.println("Error parsing conversation: " + e.getMessage());
            return Flux.error(new RuntimeException("Lỗi xử lý conversation history"));
        }
    }
}