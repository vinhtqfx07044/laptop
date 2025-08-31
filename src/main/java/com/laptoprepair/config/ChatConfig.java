package com.laptoprepair.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.PostgresChatMemoryRepositoryDialect;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Configuration class for setting up chat-related beans, including the system
 * prompt, chat memory, and ChatClient.
 */
@Configuration
public class ChatConfig {
    @Value("classpath:system-prompt.txt")
    private Resource promptResource;

    @Value("${app.chat.max-user-messages}")
    private int maxUserMessages;

    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;

    public ChatConfig(JdbcTemplate jdbcTemplate, Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        this.environment = environment;
    }

    @Bean
    public String systemPrompt() throws IOException {
        return promptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    @Bean
    public ChatMemory chatMemory() {
        JdbcChatMemoryRepository.Builder builder = JdbcChatMemoryRepository.builder()
                .jdbcTemplate(jdbcTemplate);

        // Configure dialect based on active profile
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equals(profile)) {
                builder.dialect(new PostgresChatMemoryRepositoryDialect());
                break;
            }
        }

        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(builder.build())
                .maxMessages(maxUserMessages * 3)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, String systemPrompt, ChatMemory chatMemory) {
        return builder
                .defaultSystem(systemPrompt)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}