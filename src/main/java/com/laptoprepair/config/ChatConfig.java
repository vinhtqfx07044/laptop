package com.laptoprepair.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class ChatConfig {

    @Value("classpath:chatbot-prompt.txt")
    private Resource promptResource;

    @Bean
    public String systemPrompt() throws IOException {
        return promptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, String systemPrompt) {
        return builder
                .defaultSystem(systemPrompt)
                .build();
    }
}